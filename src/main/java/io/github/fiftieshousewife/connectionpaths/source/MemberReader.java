package io.github.fiftieshousewife.connectionpaths.source;

import io.github.fiftieshousewife.connectionpaths.source.syntax.Signatures;
import io.github.fiftieshousewife.connectionpaths.source.syntax.TypeHeads;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

// Reads a type's members into the model: methods, constructors (a test class's run with each test) and initialisers,
// and which test methods are tests or set-up. JUnit 3 tests are known only once every supertype has been read, so
// they are kept as candidates until then.
final class MemberReader {

    private static final Set<String> TEST_ANNOTATIONS =
        Set.of("Test", "ParameterizedTest", "RepeatedTest", "TestFactory", "TestTemplate");
    private static final Set<String> SET_UP_ANNOTATIONS = Set.of("Before", "BeforeEach", "BeforeClass", "BeforeAll",
        "After", "AfterEach", "AfterClass", "AfterAll", "BeforeMethod", "BeforeTest", "BeforeSuite", "BeforeGroups",
        "AfterMethod", "AfterTest", "AfterSuite", "AfterGroups");
    private static final Set<String> JUNIT3_SET_UP = Set.of("setUp", "onSetUp", "onSetUpInTransaction", "tearDown",
        "onTearDown", "onTearDownInTransaction");

    // The type a member belongs to, its file, and whether it is test source.
    record Owner(String type, String file, boolean tests) {
    }

    private final SourceModel model;
    private final CallResolver calls;
    private final Map<MethodKey, SourceModel.Location> candidates = new HashMap<>();
    private final Set<MethodKey> setUpsByName = new HashSet<>();

    MemberReader(final SourceModel model, final CallResolver calls) {
        this.model = model;
        this.calls = calls;
    }

    // The JUnit 3 tests and set-ups, by their names, in the classes whose superclass is TestCase or the like.
    void addJunit3(final Predicate<String> isJunit3) {
        candidates.entrySet().stream().filter(e -> isJunit3.test(e.getKey().type()))
            .forEach(e -> model.tests().test(e.getKey(), e.getValue()));
        setUpsByName.stream().filter(key -> isJunit3.test(key.type()))
            .forEach(key -> model.tests().setUp(key.type(), key));
    }

    void read(final BodyDeclaration<?> member, final Owner owner) {
        if (member instanceof MethodDeclaration method) {
            readMethod(method, owner);
            return;
        }
        constructorParameters(member).ifPresentOrElse(parameters -> readConstructor(member, parameters, owner),
            () -> readInitialisers(member, owner));
    }

    private static Optional<String> constructorParameters(final BodyDeclaration<?> member) {
        if (member instanceof ConstructorDeclaration constructor) {
            return Optional.of(Signatures.of(constructor.getParameters()));
        }
        return member instanceof CompactConstructorDeclaration compact
            ? compact.getParentNode().filter(RecordDeclaration.class::isInstance)
                .map(record -> Signatures.of(((RecordDeclaration) record).getParameters()))
            : Optional.empty();
    }

    private void readConstructor(final BodyDeclaration<?> constructor, final String parameters, final Owner owner) {
        final MethodKey key = new MethodKey(owner.type(), MethodKey.CONSTRUCTOR, parameters);
        model.declare(key, TypeHeads.location(constructor, owner.file()));
        model.call(key, MethodKey.fieldsOf(owner.type()));
        calls.collect(key, constructor, owner.file());
        if (owner.tests()) {
            model.tests().setUp(owner.type(), key);
        }
    }

    // Static fields and blocks, and every field of an interface, run once with the type; the rest with each object.
    private void readInitialisers(final BodyDeclaration<?> member, final Owner owner) {
        if (member instanceof FieldDeclaration || member instanceof InitializerDeclaration) {
            final String type = owner.type();
            final MethodKey key = isStatic(member) ? MethodKey.staticsOf(type) : MethodKey.fieldsOf(type);
            calls.collect(key, member, owner.file());
            if (owner.tests()) {
                model.tests().mocks(key, MockedTypes.in(member));
                standIn(member).ifPresent(server -> model.tests().standIn(owner.type(), server));
            }
        }
    }

    private static Optional<String> standIn(final BodyDeclaration<?> member) {
        return member instanceof FieldDeclaration field ? StandIns.in(field) : Optional.empty();
    }

    private static boolean isStatic(final BodyDeclaration<?> member) {
        if (member instanceof FieldDeclaration field) {
            return field.isStatic() || member.getParentNode().orElse(null) instanceof ClassOrInterfaceDeclaration parent
                && parent.isInterface();
        }
        return ((InitializerDeclaration) member).isStatic();
    }

    private void readMethod(final MethodDeclaration method, final Owner owner) {
        final MethodKey key =
            new MethodKey(owner.type(), method.getNameAsString(), Signatures.of(method.getParameters()));
        final SourceModel.Location at = TypeHeads.location(method, owner.file());
        model.declare(key, at);
        if (OpenDatabases.given(method)) {
            model.mark(Mark.ON_AN_OPEN_DATABASE, key.toString());
        }
        calls.collect(key, method, owner.file());
        if (method.isStatic()) {
            model.tests().isStatic(key);
        }
        if (owner.tests()) {
            model.tests().mocks(key, MockedTypes.in(method));
            readTestMethod(method, key, at);
        }
    }

    private void readTestMethod(final MethodDeclaration method, final MethodKey key, final SourceModel.Location at) {
        if (annotated(method, TEST_ANNOTATIONS)) {
            model.tests().test(key, at);
        } else if (annotated(method, SET_UP_ANNOTATIONS)) {
            model.tests().setUp(key.type(), key);
        } else if (JUNIT3_SET_UP.contains(key.name())) {
            setUpsByName.add(key);
        } else if (isJunit3Candidate(method, key)) {
            candidates.put(key, at);
        }
    }

    private static boolean isJunit3Candidate(final MethodDeclaration method, final MethodKey key) {
        return key.name().startsWith("test") && key.parameterCount() == 0 && method.isPublic();
    }

    private static boolean annotated(final NodeWithAnnotations<?> node, final Set<String> names) {
        return node.getAnnotations().stream().anyMatch(a -> names.contains(a.getName().getIdentifier()));
    }
}
