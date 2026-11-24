package io.github.fiftieshousewife.connectionpaths.source;

import io.github.fiftieshousewife.connectionpaths.rules.ConnectionRules;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/** Reads every Java file in the source folders into a {@link SourceModel}. */
@Slf4j
public final class SourceReader {

    private static final Set<String> TEST_ANNOTATIONS =
        Set.of("Test", "ParameterizedTest", "RepeatedTest", "TestFactory", "TestTemplate");
    /** Methods that run with a test, before or after it: JUnit 4 and 5, and TestNG. */
    private static final Set<String> SET_UP_ANNOTATIONS = Set.of("Before", "BeforeEach", "BeforeClass", "BeforeAll",
        "After", "AfterEach", "AfterClass", "AfterAll", "BeforeMethod", "BeforeTest", "BeforeSuite", "BeforeGroups",
        "AfterMethod", "AfterTest", "AfterSuite", "AfterGroups");
    /** Classes whose public no-argument test...() methods are tests without an annotation (JUnit 3, Spring's). */
    private static final Set<String> JUNIT3_BASES = Set.of("TestCase", "AbstractDependencyInjectionSpringContextTests",
        "AbstractTransactionalSpringContextTests", "AbstractTransactionalDataSourceSpringContextTests",
        "AbstractJpaTests", "AbstractSpringContextTests");
    private static final Set<String> JUNIT3_SET_UP = Set.of("setUp", "onSetUp", "onSetUpInTransaction", "tearDown",
        "onTearDown", "onTearDownInTransaction");

    private final SourceModel model = new SourceModel();
    private final JavaParser parser;
    private final CallResolver calls;
    private final Map<MethodKey, SourceModel.Location> junit3Candidates = new HashMap<>();
    private final Map<String, String> superclasses = new HashMap<>();
    private final Set<String> abstractTestTypes = new HashSet<>();

    public SourceReader(final CombinedTypeSolver solver, final ConnectionRules rules) {
        this.parser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
            .setSymbolResolver(new JavaSymbolSolver(solver)));
        this.calls = new CallResolver(model, rules);
    }

    public SourceModel read(final List<Path> mainSources, final List<Path> testSources) {
        mainSources.forEach(dir -> readFolder(dir, false));
        testSources.forEach(dir -> readFolder(dir, true));
        junit3Candidates.forEach((method, at) -> {
            if (model.ancestors(method.type()).stream().anyMatch(SourceReader::isJunit3Base)) {
                model.test(method, at);
            }
        });
        InheritedTests.spread(model, abstractTestTypes);
        superclasses.forEach((type, superclass) -> {
            // Creating an object first runs its superclass's constructor (super() unless it says otherwise), and so
            // the superclass's field initialisers.
            model.call(MethodKey.fieldsOf(type), new MethodKey(superclass, MethodKey.CONSTRUCTOR, ""));
            model.call(MethodKey.fieldsOf(type), MethodKey.fieldsOf(superclass));
        });
        return model;
    }

    private static boolean isJunit3Base(final String type) {
        return JUNIT3_BASES.contains(type.substring(type.lastIndexOf('.') + 1));
    }

    private void readFolder(final Path dir, final boolean tests) {
        if (!Files.isDirectory(dir)) {
            log.warn("Not a folder, skipped: {}", dir);
            return;
        }
        for (final Path file : javaFiles(dir)) {
            model.getCounts().files++;
            final ParseResult<CompilationUnit> result = parse(file);
            if (!result.isSuccessful() || result.getResult().isEmpty()) {
                model.getCounts().filesNotParsed++;
                log.warn("Not parsed, so left out: {} ({})", file,
                result.getProblems().isEmpty() ? "no result" : result.getProblems().get(0).getVerboseMessage());
                continue;
            }
            final String name = file.toString().replace('\\', '/');
            result.getResult().get().findAll(TypeDeclaration.class).forEach(type -> readType(type, name, tests));
        }
    }

    private static List<Path> javaFiles(final Path dir) {
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + dir, e);
        }
    }

    private ParseResult<CompilationUnit> parse(final Path file) {
        try {
            return parser.parse(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + file, e);
        }
    }

    private void readType(final TypeDeclaration<?> type, final String file, final boolean tests) {
        final String name = type.getFullyQualifiedName().orElse(null);
        if (name == null) {
            return;
        }
        supertypes(type).forEach(parent -> model.supertype(name, parent));
        superclass(type).ifPresent(parent -> superclasses.put(name, parent));
        if (tests) {
            model.testType(name);
            if (type instanceof ClassOrInterfaceDeclaration declaration && declaration.isAbstract()) {
                abstractTestTypes.add(name);
            }
            nestedTestClass(type).ifPresent(outer -> model.nestedIn(name, outer));
            Frameworks.startedBy(type).ifPresent(annotation -> model.startsAFramework(name, annotation));
        }
        model.declare(MethodKey.fieldsOf(name), location(type, file));
        model.declare(MethodKey.staticsOf(name), location(type, file));
        final Owner owner = new Owner(name, file, tests);
        if (type instanceof EnumDeclaration declaration) {
            declaration.getEntries().forEach(entry -> calls.collect(MethodKey.staticsOf(name), entry, file));
        }
        for (final BodyDeclaration<?> member : type.getMembers()) {
            readMember(member, owner);
        }
    }

    private static Optional<String> superclass(final TypeDeclaration<?> type) {
        if (type instanceof ClassOrInterfaceDeclaration declaration && !declaration.isInterface()) {
            return declaration.getExtendedTypes().stream().findFirst().map(SourceReader::qualifiedName);
        }
        return Optional.empty();
    }

    private static Optional<String> nestedTestClass(final TypeDeclaration<?> type) {
        if (!type.isAnnotationPresent("Nested")) {
            return Optional.empty();
        }
        return type.getParentNode()
            .filter(TypeDeclaration.class::isInstance)
            .flatMap(parent -> ((TypeDeclaration<?>) parent).getFullyQualifiedName());
    }

    /** The type a member belongs to, its file, and whether it is test source. */
    private record Owner(String type, String file, boolean tests) {
    }

    private void readMember(final BodyDeclaration<?> member, final Owner owner) {
        if (member instanceof MethodDeclaration method) {
            readMethod(method, owner);
        } else if (member instanceof ConstructorDeclaration constructor) {
            readConstructor(constructor, Signatures.of(constructor.getParameters()), owner);
        } else if (member instanceof CompactConstructorDeclaration compact
            && compact.getParentNode().orElse(null) instanceof RecordDeclaration record) {
            readConstructor(compact, Signatures.of(record.getParameters()), owner);
        } else if (member instanceof FieldDeclaration || member instanceof InitializerDeclaration) {
            calls.collect(initialisersOf(member, owner.type()), member, owner.file());
        }
    }

    private void readConstructor(final BodyDeclaration<?> constructor, final String parameters, final Owner owner) {
        final MethodKey key = new MethodKey(owner.type(), MethodKey.CONSTRUCTOR, parameters);
        model.declare(key, location(constructor, owner.file()));
        model.call(key, MethodKey.fieldsOf(owner.type()));
        calls.collect(key, constructor, owner.file());
        if (owner.tests()) {
            model.setUp(owner.type(), key);
        }
    }

    /** Static fields and blocks, and every field of an interface, run once with the type; the rest with each object. */
    private static MethodKey initialisersOf(final BodyDeclaration<?> member, final String type) {
        final boolean inInterface = member.getParentNode().orElse(null) instanceof ClassOrInterfaceDeclaration parent
            && parent.isInterface();
        final boolean isStatic = member instanceof FieldDeclaration field ? field.isStatic() || inInterface
            : ((InitializerDeclaration) member).isStatic();
        return isStatic ? MethodKey.staticsOf(type) : MethodKey.fieldsOf(type);
    }

    private void readMethod(final MethodDeclaration method, final Owner owner) {
        final MethodKey key =
            new MethodKey(owner.type(), method.getNameAsString(), Signatures.of(method.getParameters()));
        final SourceModel.Location at = location(method, owner.file());
        model.declare(key, at);
        calls.collect(key, method, owner.file());
        if (!owner.tests()) {
            return;
        }
        if (annotated(method, TEST_ANNOTATIONS)) {
            model.test(key, at);
        } else if (annotated(method, SET_UP_ANNOTATIONS) || JUNIT3_SET_UP.contains(key.name())) {
            model.setUp(owner.type(), key);
        } else if (key.name().startsWith("test") && key.parameterCount() == 0 && method.isPublic()) {
            junit3Candidates.put(key, at);
        }
    }

    private static boolean annotated(final NodeWithAnnotations<?> node, final Set<String> names) {
        return node.getAnnotations().stream().anyMatch(a -> names.contains(a.getName().getIdentifier()));
    }

    private static SourceModel.Location location(final com.github.javaparser.ast.Node node, final String file) {
        return new SourceModel.Location(file, node.getBegin().map(p -> p.line).orElse(0));
    }

    private static List<String> supertypes(final TypeDeclaration<?> type) {
        final List<ClassOrInterfaceType> declared = new ArrayList<>();
        if (type instanceof ClassOrInterfaceDeclaration declaration) {
            declared.addAll(declaration.getExtendedTypes());
            declared.addAll(declaration.getImplementedTypes());
        } else if (type instanceof EnumDeclaration declaration) {
            declared.addAll(declaration.getImplementedTypes());
        } else if (type instanceof RecordDeclaration declaration) {
            declared.addAll(declaration.getImplementedTypes());
        }
        return declared.stream().map(SourceReader::qualifiedName).toList();
    }

    /** A supertype's full name, or {@code ?.Name} where it can't be resolved. */
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingThrowable"})
    private static String qualifiedName(final ClassOrInterfaceType type) {
        try {
            return type.resolve().asReferenceType().getQualifiedName();
        } catch (RuntimeException | StackOverflowError e) {
            return "?." + type.getNameAsString();
        }
    }
}
