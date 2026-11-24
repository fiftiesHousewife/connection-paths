package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.address.Locality;
import io.github.fiftieshousewife.connectionpaths.graph.CallGraph;
import io.github.fiftieshousewife.connectionpaths.graph.TestChains;
import io.github.fiftieshousewife.connectionpaths.settings.SettingsFiles;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Turns the source model and call graph into {@link Facts}. */
public final class FactsBuilder {

    private static final int REASONS_REPORTED = 25;

    private final SourceModel model;
    private final CallGraph graph;
    private final SettingsFiles settings;
    private final MockedChains mocks;
    private final TestAddresses testAddresses;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "These are shared on purpose")
    public FactsBuilder(final SourceModel model, final CallGraph graph, final SettingsFiles settings) {
        this.model = model;
        this.graph = graph;
        this.settings = settings;
        this.mocks = new MockedChains(model);
        this.testAddresses = new TestAddresses(model, settings);
    }

    /** The facts, naming the tool's version and what happened to the classpath's entries. */
    public Facts build(final String toolVersion, final List<String> classpathNotes) {
        final List<Facts.Point> points = points();
        final SystemsBuilder.Result systems = new SystemsBuilder(model, graph).build(points);
        final List<Facts.TestReach> tests = testsReaching(reachedByKind(), systems.byTest());
        final List<Facts.FrameworkTest> frameworkTests = testsStartingAFramework();
        return Facts.builder()
            .toolVersion(toolVersion)
            .summary(summary(tests.size(), frameworkTests.size()))
            .connectionPoints(points)
            .systems(systems.systems())
            .tests(tests)
            .testsStartingAFramework(frameworkTests)
            .notResolved(model.getUnresolved().top(REASONS_REPORTED))
            .dispatchPointsNotFollowed(graph.getNotFollowed().entrySet().stream()
                .map(e -> new Facts.Dispatch(e.getKey(), e.getValue())).toList())
            .classpathNotes(classpathNotes)
            .build();
    }

    // For each kind of connection, everything that reaches one, walked back from its points.
    private Map<String, Map<MethodKey, MethodKey>> reachedByKind() {
        final Map<String, Set<MethodKey>> startsByKind = new TreeMap<>();
        model.getPoints().forEach(p -> startsByKind.computeIfAbsent(p.kind(), k -> new TreeSet<>()).add(p.method()));
        final Map<String, Map<MethodKey, MethodKey>> reachedByKind = new TreeMap<>();
        startsByKind.forEach((kind, starts) -> reachedByKind.put(kind, graph.reachBack(starts)));
        return reachedByKind;
    }

    private Facts.Summary summary(final int testsReaching, final int testsStartingAFramework) {
        return Facts.Summary.builder()
            .files(model.getCounts().getFiles())
            .filesNotParsed(model.getCounts().getFilesNotParsed())
            .methods((int) model.getDeclared().stream().filter(m -> !m.isInitialisers()).count())
            .callsResolved(model.getCounts().getCallsResolved())
            .callsNotResolved(model.getUnresolved().getTotal())
            .tests(model.tests().all().size())
            .testsReaching(testsReaching)
            .testsStartingAFramework(testsStartingAFramework)
            .build();
    }

    private List<Facts.Point> points() {
        return model.getPoints().stream()
            .sorted((a, b) -> (a.method() + a.call()).compareTo(b.method() + b.call()))
            .map(p -> {
                final SourceModel.Location at = model.locationOf(p);
                final Address address = settings.withValue(model.addressOf(p));
                return Facts.Point.builder().kind(p.kind()).call(p.call()).method(p.method().toString())
                    .file(at.file()).line(at.line()).address(address).local(localBecause(p, address)).build();
            })
            .toList();
    }

    // Why a point stays on this machine, if it does. A point whose address is a parameter no code in the source passes
    // can't be followed further, and is left to a person, as local.
    private String localBecause(final SourceModel.ConnectionPoint point, final Address address) {
        final Optional<String> local = Locality.of(address, point.kind());
        if (local.isPresent()) {
            return local.get();
        }
        final boolean parameterNobodyPasses = Address.PARAMETER.equals(address.getSource())
            && graph.reachBack(Set.of(point.method())).size() == 1;
        return parameterNobodyPasses ? "its address is a parameter no code in the source passes" : null;
    }

    private List<Facts.FrameworkTest> testsStartingAFramework() {
        final List<Facts.FrameworkTest> found = new ArrayList<>();
        model.tests().all().forEach((test, at) -> model.tests().frameworkOf(test.type()).ifPresent(framework ->
            found.add(Facts.FrameworkTest.builder().test(TestChains.nameOf(test)).file(at.file()).line(at.line())
                .framework(framework.annotation()).annotatedOn(framework.on()).build())));
        return found;
    }

    private List<Facts.TestReach> testsReaching(final Map<String, Map<MethodKey, MethodKey>> reachedByKind,
                                                final Map<MethodKey, List<String>> systemsByTest) {
        final List<Facts.TestReach> tests = new ArrayList<>();
        model.tests().all().forEach((test, at) -> {
            final Facts.TestReach.TestReachBuilder entry = Facts.TestReach.builder()
                .test(TestChains.nameOf(test)).file(at.file()).line(at.line())
                .systems(systemsByTest.getOrDefault(test, List.of()))
                .standIns(model.tests().standInsOf(test.type()));
            final List<Facts.Reach> reaches = reachedByKind.entrySet().stream()
                .map(kind -> reach(test, kind.getKey(), kind.getValue())).flatMap(Optional::stream).toList();
            if (!reaches.isEmpty()) {
                reaches.forEach(entry::reach);
                tests.add(entry.build());
            }
        });
        return tests;
    }

    // How a test reaches a kind of connection: through its own body first, otherwise through its set-up.
    private Optional<Facts.Reach> reach(final MethodKey test, final String kind, final Map<MethodKey, MethodKey> next) {
        return TestChains.of(model, test, next).map(c -> reachOf(test, kind, c));
    }

    private Facts.Reach reachOf(final MethodKey test, final String kind, final TestChains.TestChain chain) {
        final List<MethodKey> steps = chain.steps();
        final MethodKey point = steps.get(steps.size() - 1);
        return Facts.Reach.builder()
            .kind(kind)
            .point(point.toString())
            .throughSetUp(chain.throughSetUp())
            .throughMock(mocks.throughMock(test, steps).map(MethodKey::toString).orElse(null))
            .addressForTests(testAddresses.of(steps, kind))
            .chain(steps.stream().map(MethodKey::toString).toList())
            .build();
    }
}
