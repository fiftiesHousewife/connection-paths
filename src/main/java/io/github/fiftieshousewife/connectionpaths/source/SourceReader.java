package io.github.fiftieshousewife.connectionpaths.source;

import io.github.fiftieshousewife.connectionpaths.rules.ConnectionRules;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/** Reads every Java file in the source folders into a {@link SourceModel}. */
@Slf4j
public final class SourceReader {

    private final SourceModel model = new SourceModel();
    private final JavaParser parser;
    private final CallResolver calls;
    private final MemberReader members;
    private final Map<String, String> superclasses = new HashMap<>();
    private final Set<String> abstractTestTypes = new HashSet<>();

    /** A reader that resolves calls with this solver, and finds connections by these rules. */
    public SourceReader(final CombinedTypeSolver solver, final ConnectionRules rules) {
        this.parser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
            .setSymbolResolver(new JavaSymbolSolver(solver)));
        this.calls = new CallResolver(model, rules);
        this.members = new MemberReader(model, calls);
    }

    /** Reads the production and test source folders. */
    public SourceModel read(final List<Path> mainSources, final List<Path> testSources) {
        mainSources.forEach(dir -> readFolder(dir, false));
        testSources.forEach(dir -> readFolder(dir, true));
        members.junit3Candidates().entrySet().stream()
            .filter(e -> model.types().ancestors(e.getKey().type()).stream().anyMatch(TypeHeads::isJunit3Base))
            .forEach(e -> model.tests().test(e.getKey(), e.getValue()));
        InheritedTests.spread(model, abstractTestTypes);
        // Creating an object first runs its superclass's constructor (super() unless it says otherwise), and so the
        // superclass's field initialisers.
        superclasses.forEach((type, superclass) -> {
            model.call(MethodKey.fieldsOf(type), new MethodKey(superclass, MethodKey.CONSTRUCTOR, ""));
            model.call(MethodKey.fieldsOf(type), MethodKey.fieldsOf(superclass));
        });
        return model;
    }

    private void readFolder(final Path dir, final boolean tests) {
        if (Files.isDirectory(dir)) {
            javaFiles(dir).forEach(file -> readFile(file, tests));
        } else {
            log.warn("Not a folder, skipped: {}", dir);
        }
    }

    private void readFile(final Path file, final boolean tests) {
        model.getCounts().files++;
        final ParseResult<CompilationUnit> result = parse(file);
        if (result.isSuccessful() && result.getResult().isPresent()) {
            final String name = file.toString().replace('\\', '/');
            final CompilationUnit unit = result.getResult().get();
            unit.findAll(TypeDeclaration.class).forEach(type -> readType(type, name, tests));
            CreatedTypes.namedInStrings(unit).forEach(model::created);
            return;
        }
        model.getCounts().filesNotParsed++;
        log.warn("Not parsed, so left out: {} ({})", file,
            result.getProblems().isEmpty() ? "no result" : result.getProblems().get(0).getVerboseMessage());
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
        type.getFullyQualifiedName().ifPresent(name -> {
            TypeHeads.supertypes(type).forEach(parent -> model.types().supertype(name, parent));
            TypeHeads.superclass(type).ifPresent(parent -> superclasses.put(name, parent));
            if (CreatedTypes.isCreatedWithoutNew(type, tests)) {
                model.created(name);
            }
            if (tests) {
                readTestType(type, name);
            }
            model.declare(MethodKey.fieldsOf(name), TypeHeads.location(type, file));
            model.declare(MethodKey.staticsOf(name), TypeHeads.location(type, file));
            if (type instanceof EnumDeclaration declaration) {
                declaration.getEntries().forEach(entry -> calls.collect(MethodKey.staticsOf(name), entry, file));
            }
            final MemberReader.Owner owner = new MemberReader.Owner(name, file, tests);
            type.getMembers().forEach(member -> members.read(member, owner));
            readAnonymousClasses(type, owner);
        });
    }

    // Each anonymous class is a type of its own, whose supertype is what it implements, so its methods are reached
    // through that type's.
    private void readAnonymousClasses(final Node maker, final MemberReader.Owner makerOwner) {
        AnonymousClasses.madeIn(maker, makerOwner.type()).forEach(anonymous -> {
            final ObjectCreationExpr creation = anonymous.creation();
            final MemberReader.Owner owner =
                new MemberReader.Owner(anonymous.name(), makerOwner.file(), makerOwner.tests());
            model.types().supertype(anonymous.name(), TypeHeads.qualifiedName(creation.getType()));
            model.created(anonymous.name());
            model.declare(MethodKey.fieldsOf(anonymous.name()), TypeHeads.location(creation, owner.file()));
            creation.getAnonymousClassBody().orElseThrow().forEach(member -> members.read(member, owner));
            readAnonymousClasses(creation, owner);
        });
    }

    private void readTestType(final TypeDeclaration<?> type, final String name) {
        model.tests().testType(name);
        if (TypeHeads.isAbstractClass(type)) {
            abstractTestTypes.add(name);
        }
        TypeHeads.nestedTestClassIn(type).ifPresent(outer -> model.types().nestedIn(name, outer));
        Frameworks.startedBy(type).ifPresent(annotation -> model.tests().startsAFramework(name, annotation));
    }
}
