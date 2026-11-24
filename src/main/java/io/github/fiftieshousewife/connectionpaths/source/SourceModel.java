package io.github.fiftieshousewife.connectionpaths.source;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Everything read from the source: methods, the calls each makes, types' supertypes, tests, and connection points. */
public final class SourceModel {

    /** A place in a source file. */
    public record Location(String file, int line) {
    }

    /** A method that makes a call the rules say opens or uses a connection. */
    public record ConnectionPoint(String kind, String call, MethodKey method) {
    }

    private final Set<MethodKey> declared = new HashSet<>();
    private final Map<MethodKey, Set<MethodKey>> calls = new HashMap<>();
    private final Map<MethodKey, Location> locations = new HashMap<>();
    private final Set<ConnectionPoint> points = new LinkedHashSet<>();
    private final Set<String> createdTypes = new HashSet<>();
    private final Map<ConnectionPoint, Location> pointLocations = new HashMap<>();
    private final Map<ConnectionPoint, Address> pointAddresses = new HashMap<>();
    private final TypeHierarchy hierarchy = new TypeHierarchy();
    private final TestModel testModel = new TestModel(hierarchy, declared);
    private final Unresolved unresolved = new Unresolved();
    private final Counts counts = new Counts();

    /** How much was read. */
    @lombok.Getter
    public static final class Counts {
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

    /** A connection point, at the first call in its method that makes it one. */
    public void point(final ConnectionPoint point, final Location at, final Address address) {
        points.add(point);
        pointLocations.putIfAbsent(point, at);
        pointAddresses.putIfAbsent(point, address);
    }

    /** A type the program creates objects of, with new, by name, or through a framework. */
    public void created(final String type) {
        createdTypes.add(type);
    }

    /** The types the program creates objects of. */
    public Set<String> getCreated() {
        return Collections.unmodifiableSet(createdTypes);
    }

    /** The supertypes of the types in the source. */
    public TypeHierarchy types() {
        return hierarchy;
    }

    /** The tests, and what runs with them. */
    public TestModel tests() {
        return testModel;
    }

    /** Every method the source declares, with its pseudo-methods for initialisers. */
    public Set<MethodKey> getDeclared() {
        return Collections.unmodifiableSet(declared);
    }

    public Map<MethodKey, Set<MethodKey>> getCalls() {
        return Collections.unmodifiableMap(calls);
    }

    Location locationOf(final MethodKey method) {
        return locations.get(method);
    }

    public Location locationOf(final ConnectionPoint point) {
        return pointLocations.get(point);
    }

    public Address addressOf(final ConnectionPoint point) {
        return pointAddresses.get(point);
    }

    public Set<ConnectionPoint> getPoints() {
        return Collections.unmodifiableSet(points);
    }

    public Unresolved getUnresolved() {
        return unresolved;
    }

    public Counts getCounts() {
        return counts;
    }
}
