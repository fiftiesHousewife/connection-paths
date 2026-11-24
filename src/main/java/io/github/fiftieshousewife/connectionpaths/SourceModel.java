package io.github.fiftieshousewife.connectionpaths;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Everything read from the source: methods, the calls each makes, types' supertypes, tests, and connection points. */
final class SourceModel {

    /** A place in a source file. */
    record Location(String file, int line) {
    }

    /** A method that makes a call the rules say opens or uses a connection. */
    record ConnectionPoint(String kind, String call, MethodKey method) {
    }

    private final Set<MethodKey> declared = new HashSet<>();
    private final Map<MethodKey, Set<MethodKey>> calls = new HashMap<>();
    private final Map<String, Set<String>> parents = new HashMap<>();
    private final Map<MethodKey, Location> locations = new HashMap<>();
    private final Map<MethodKey, Location> tests = new TreeMap<>();
    private final Map<String, Set<MethodKey>> setUps = new HashMap<>();
    private final Set<ConnectionPoint> points = new LinkedHashSet<>();
    private final Map<ConnectionPoint, Location> pointLocations = new HashMap<>();
    private final Map<ConnectionPoint, Address> pointAddresses = new HashMap<>();
    private final Set<String> testTypes = new HashSet<>();
    private final Map<String, String> enclosing = new HashMap<>();
    private final Map<String, String> frameworks = new HashMap<>();
    private final Unresolved unresolved = new Unresolved();
    private final Counts counts = new Counts();

    /** How much was read. */
    static final class Counts {
        int files;
        int filesNotParsed;
        int callsResolved;
    }

    void declare(final MethodKey method, final Location at) {
        declared.add(method);
        locations.putIfAbsent(method, at);
        calls.computeIfAbsent(method, k -> new HashSet<>());
    }

    void call(final MethodKey caller, final MethodKey callee) {
        calls.computeIfAbsent(caller, k -> new HashSet<>()).add(callee);
    }

    void supertype(final String type, final String parent) {
        parents.computeIfAbsent(type, k -> new HashSet<>()).add(parent);
    }

    void test(final MethodKey test, final Location at) {
        tests.put(test, at);
    }

    void setUp(final String type, final MethodKey method) {
        setUps.computeIfAbsent(type, k -> new HashSet<>()).add(method);
    }

    /** A connection point, at the first call in its method that makes it one. */
    void point(final ConnectionPoint point, final Location at, final Address address) {
        points.add(point);
        pointLocations.putIfAbsent(point, at);
        pointAddresses.putIfAbsent(point, address);
    }

    void testType(final String type) {
        testTypes.add(type);
    }

    boolean isTestType(final String type) {
        return testTypes.contains(type);
    }

    /** A nested test class runs inside an instance of the class around it, with that class's set-up. */
    void nestedIn(final String inner, final String outer) {
        enclosing.put(inner, outer);
    }

    /** A test class, or a class tests extend, that starts a framework by this annotation. */
    void startsAFramework(final String type, final String annotation) {
        frameworks.put(type, annotation);
    }

    /** A framework a test class starts: by an annotation on it, its superclasses, or the classes around it. */
    Optional<Framework> frameworkOf(final String testClass) {
        for (String type = testClass; type != null; type = enclosing.get(type)) {
            final Set<String> withAncestors = new LinkedHashSet<>(List.of(type));
            withAncestors.addAll(ancestors(type));
            for (final String each : withAncestors) {
                if (frameworks.containsKey(each)) {
                    return Optional.of(new Framework(frameworks.get(each), each));
                }
            }
        }
        return Optional.empty();
    }

    /** The annotation that starts a framework, and the class it is on. */
    record Framework(String annotation, String on) {
    }

    /** Every supertype of a type, following the supertypes recorded for the types in the source. */
    Set<String> ancestors(final String type) {
        final Set<String> seen = new LinkedHashSet<>();
        final java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>(parents.getOrDefault(type, Set.of()));
        while (!queue.isEmpty()) {
            final String next = queue.pop();
            if (seen.add(next)) {
                queue.addAll(parents.getOrDefault(next, Set.of()));
            }
        }
        return seen;
    }

    /**
     * What runs with each test besides the test itself: its class's and superclasses' set-up and tear-down methods,
     * constructors and initialisers, and those of the classes a nested test class sits in.
     */
    Set<MethodKey> setUpOf(final String testClass) {
        final Set<MethodKey> all = new TreeSet<>();
        for (String type = testClass; type != null; type = enclosing.get(type)) {
            final Set<String> withAncestors = new LinkedHashSet<>(List.of(type));
            withAncestors.addAll(ancestors(type));
            for (final String each : withAncestors) {
                all.addAll(setUps.getOrDefault(each, Set.of()));
                all.add(MethodKey.fieldsOf(each));
                all.add(MethodKey.staticsOf(each));
            }
        }
        all.retainAll(declared);
        return all;
    }

    Set<MethodKey> getDeclared() {
        return Collections.unmodifiableSet(declared);
    }

    Map<MethodKey, Set<MethodKey>> getCalls() {
        return Collections.unmodifiableMap(calls);
    }

    Map<MethodKey, Location> getTests() {
        return Collections.unmodifiableMap(tests);
    }

    Location locationOf(final MethodKey method) {
        return locations.get(method);
    }

    Location locationOf(final ConnectionPoint point) {
        return pointLocations.get(point);
    }

    Address addressOf(final ConnectionPoint point) {
        return pointAddresses.get(point);
    }

    Set<ConnectionPoint> getPoints() {
        return Collections.unmodifiableSet(points);
    }

    Unresolved getUnresolved() {
        return unresolved;
    }

    Counts getCounts() {
        return counts;
    }
}
