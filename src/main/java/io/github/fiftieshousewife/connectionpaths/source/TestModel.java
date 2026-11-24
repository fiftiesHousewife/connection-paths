package io.github.fiftieshousewife.connectionpaths.source;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/** The tests read from the source, and what runs with each: set-up, initialisers, and any framework it starts. */
public final class TestModel {

    /** The annotation that starts a framework, and the class it is on. */
    public record Framework(String annotation, String on) {
    }

    private final TypeHierarchy types;
    private final Set<MethodKey> declared;
    private final Map<MethodKey, SourceModel.Location> tests = new TreeMap<>();
    private final Map<MethodKey, MethodKey> inherited = new HashMap<>();
    private final Map<String, Set<MethodKey>> setUps = new HashMap<>();
    private final Set<String> testTypeNames = new HashSet<>();
    private final Map<String, String> frameworks = new HashMap<>();

    TestModel(final TypeHierarchy types, final Set<MethodKey> declared) {
        this.types = types;
        this.declared = declared;
    }

    void test(final MethodKey test, final SourceModel.Location at) {
        tests.put(test, at);
    }

    // A test a subclass runs, declared in one of its superclasses; it runs with the subclass's set-up.
    void inheritedTest(final MethodKey runAs, final MethodKey declaredAs, final SourceModel.Location at) {
        tests.put(runAs, at);
        inherited.put(runAs, declaredAs);
    }

    // A test declared in an abstract class, which only its subclasses run.
    void dropTest(final MethodKey test) {
        tests.remove(test);
    }

    void setUp(final String type, final MethodKey method) {
        setUps.computeIfAbsent(type, k -> new HashSet<>()).add(method);
    }

    void testType(final String type) {
        testTypeNames.add(type);
    }

    // A test class, or a class tests extend, that starts a framework by this annotation.
    void startsAFramework(final String type, final String annotation) {
        frameworks.put(type, annotation);
    }

    /** Every test, by its name and where it is. */
    public Map<MethodKey, SourceModel.Location> all() {
        return Collections.unmodifiableMap(tests);
    }

    Set<String> testTypes() {
        return Collections.unmodifiableSet(testTypeNames);
    }

    /** Whether a type is in the test source. */
    public boolean isTestType(final String type) {
        return testTypeNames.contains(type);
    }

    /** The method that declares a test: itself, or for an inherited test, its superclass's. */
    public MethodKey declarationOf(final MethodKey test) {
        return inherited.getOrDefault(test, test);
    }

    /** A framework a test class starts: by an annotation on it, its superclasses, or the classes around it. */
    public Optional<Framework> frameworkOf(final String testClass) {
        return types.runningWith(testClass).stream().filter(frameworks::containsKey).findFirst()
            .map(type -> new Framework(frameworks.get(type), type));
    }

    /**
     * What runs with each test besides the test itself: its class's and superclasses' set-up and tear-down methods,
     * constructors and initialisers, and those of the classes a nested test class sits in.
     */
    public Set<MethodKey> setUpOf(final String testClass) {
        final Set<MethodKey> all = new TreeSet<>();
        types.runningWith(testClass).stream()
            .flatMap(type -> Stream.concat(setUps.getOrDefault(type, Set.of()).stream(),
                Stream.of(MethodKey.fieldsOf(type), MethodKey.staticsOf(type))))
            .filter(declared::contains)
            .forEach(all::add);
        return all;
    }
}
