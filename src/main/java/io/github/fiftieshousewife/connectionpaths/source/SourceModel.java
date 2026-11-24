package io.github.fiftieshousewife.connectionpaths.source;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import com.github.javaparser.ast.CompilationUnit;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/** Everything read from the source: methods, the calls each makes, types' supertypes, tests, and connection points. */
public final class SourceModel {

    /** A place in a source file. */
    public record Location(String file, int line) {
    }

    /** A method that makes a call the rules say opens or uses a connection. */
    public record ConnectionPoint(String kind, String call, MethodKey method) {
    }

    /** One call that makes a method a connection point: where it is, and the address it is given. */
    public record PointCall(Location at, Address address) {
    }

    private final Set<MethodKey> declared = new HashSet<>();
    private final Map<MethodKey, Set<MethodKey>> calls = new HashMap<>();
    private final Map<MethodKey, Location> locations = new HashMap<>();
    private final Set<ConnectionPoint> points = new LinkedHashSet<>();
    private final Set<String> createdTypes = new HashSet<>();
    private final Map<MethodKey, Set<String>> typesCreatedBy = new HashMap<>();
    private final Set<String> onAnOpenDatabase = new HashSet<>();
    private final Map<String, Optional<CompilationUnit>> rereadFiles = new HashMap<>();
    private Function<Path, Optional<CompilationUnit>> parser = file -> Optional.empty();
    private final Map<ConnectionPoint, Map<Location, Address>> pointCalls = new HashMap<>();
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

    /** A connection point, at one of the calls in its method that make it one. */
    public void point(final ConnectionPoint point, final Location at, final Address address) {
        points.add(point);
        pointCalls.computeIfAbsent(point, k -> new LinkedHashMap<>()).putIfAbsent(at, address);
    }

    void givenAnOpenDatabase(final String code) {
        onAnOpenDatabase.add(code);
    }

    /** The methods, and calls ({@code caller > callee}), that run only once a database connection is open. */
    public Set<String> givenAnOpenDatabase() {
        return Collections.unmodifiableSet(onAnOpenDatabase);
    }

    void rereadWith(final Function<Path, Optional<CompilationUnit>> fileParser) {
        this.parser = fileParser;
    }

    /** A source file read again, with calls resolved as when it was first read, to look closer at one of its calls. */
    public Optional<CompilationUnit> reread(final String file) {
        return rereadFiles.computeIfAbsent(file, name -> parser.apply(Path.of(name)));
    }

    /** A type the program creates objects of, with new, by name, or through a framework. */
    public void created(final String type) {
        createdTypes.add(type);
    }

    // A type created with new in a method or initialiser, which is created only if that code runs.
    void createdBy(final MethodKey creator, final String type) {
        testModel.createdIn(creator.type(), type);
        typesCreatedBy.computeIfAbsent(creator, k -> new HashSet<>()).add(type);
    }

    /** The types created: by code that runs whatever else does, or a created type's; with or without tests' code. */
    public Set<String> created(final boolean withTests) {
        return CreatedTypes.live(this, createdTypes,
            withTests ? typesCreatedBy : CreatedTypes.outsideTests(testModel, typesCreatedBy));
    }

    /** The supertypes of the types in the source. */
    public TypeHierarchy types() {
        return hierarchy;
    }

    /** The tests, and what runs with them. */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The model is shared on purpose")
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

    /** Where a method is declared. */
    public Location locationOf(final MethodKey method) {
        return locations.get(method);
    }

    /** Each call that makes a method this connection point, in the order they were read. */
    public List<PointCall> callsOf(final ConnectionPoint point) {
        return pointCalls.getOrDefault(point, Map.of()).entrySet().stream()
            .map(e -> new PointCall(e.getKey(), e.getValue())).toList();
    }

    public Set<ConnectionPoint> getPoints() {
        return Collections.unmodifiableSet(points);
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The model is shared on purpose")
    public Unresolved getUnresolved() {
        return unresolved;
    }

    public Counts getCounts() {
        return counts;
    }
}
