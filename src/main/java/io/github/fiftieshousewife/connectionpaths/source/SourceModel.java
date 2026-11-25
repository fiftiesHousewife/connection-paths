package io.github.fiftieshousewife.connectionpaths.source;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import com.github.javaparser.ast.CompilationUnit;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

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
    private final Map<Mark, Set<String>> marks = new EnumMap<>(Mark.class);
    private final Map<String, Optional<CompilationUnit>> rereadFiles = new HashMap<>();
    private Function<Path, Optional<CompilationUnit>> parser = file -> Optional.empty();
    private final Map<ConnectionPoint, Map<Location, Address>> pointCalls = new HashMap<>();
    private final TypeHierarchy hierarchy = new TypeHierarchy();
    private final TestModel testModel = new TestModel(hierarchy, declared);
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The model is shared on purpose"))
    private final Unresolved unresolved = new Unresolved();
    @Getter
    private final Counts counts = new Counts();

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

    public void mark(final Mark mark, final String code) {
        marks.computeIfAbsent(mark, k -> new HashSet<>()).add(code);
    }

    /** The methods, and calls ({@code caller > callee}), the source says this of. */
    public Set<String> marked(final Mark mark) {
        return Collections.unmodifiableSet(marks.getOrDefault(mark, Set.of()));
    }

    /** A bean in a Spring XML file, which code that loads the file as a context makes. */
    public void xmlContextHolds(final String file, final MethodKey bean) {
        call(XmlContexts.contextOf(file), bean);
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
        typesCreatedBy.computeIfAbsent(creator, k -> new HashSet<>()).add(type);
    }

    /** The types created: by code that runs whatever else does, or a created type's, the tests' code among it. */
    public Set<String> created() {
        return CreatedTypes.live(this, createdTypes, typesCreatedBy);
    }

    /** The types created as a test class's tests run: by its own code, and the code outside tests that then runs. */
    public Set<String> createdFor(final String testClass) {
        return CreatedTypes.live(this, createdTypes, CreatedTypes.forATest(this, typesCreatedBy, testClass));
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

    /** The connection points, but for those marked as no point, as a call into the source's own client is. */
    public Set<ConnectionPoint> getPoints() {
        final Set<String> not = marked(Mark.NOT_A_POINT);
        return points.stream().filter(p -> !not.contains(p.toString())).collect(Collectors.toCollection(
            LinkedHashSet::new));
    }
}
