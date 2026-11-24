package io.github.fiftieshousewife.connectionpaths;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Turns the source model and call graph into {@link Facts}. */
final class FactsBuilder {

    private static final int REASONS_REPORTED = 25;

    private final SourceModel model;
    private final CallGraph graph;
    private final SettingsFiles settings;

    FactsBuilder(final SourceModel model, final CallGraph graph, final SettingsFiles settings) {
        this.model = model;
        this.graph = graph;
        this.settings = settings;
    }

    Facts build(final String toolVersion, final List<String> classpathNotes) {
        final Map<String, Set<MethodKey>> startsByKind = new TreeMap<>();
        model.getPoints().forEach(p -> startsByKind.computeIfAbsent(p.kind(), k -> new TreeSet<>()).add(p.method()));
        final Map<String, Map<MethodKey, MethodKey>> reachedByKind = new TreeMap<>();
        startsByKind.forEach((kind, starts) -> reachedByKind.put(kind, graph.reachBack(starts)));
        final List<Facts.Point> points = points();
        final SystemsBuilder.Result systems = new SystemsBuilder(model, graph).build(points);
        final List<Facts.TestReach> tests = testsReaching(reachedByKind, systems.byTest());
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

    private Facts.Summary summary(final int testsReaching, final int testsStartingAFramework) {
        return Facts.Summary.builder()
            .files(model.getCounts().files)
            .filesNotParsed(model.getCounts().filesNotParsed)
            .methods((int) model.getDeclared().stream().filter(m -> !m.isInitialisers()).count())
            .callsResolved(model.getCounts().callsResolved)
            .callsNotResolved(model.getUnresolved().getTotal())
            .tests(model.getTests().size())
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

    /**
     * Why a point stays on this machine, if it does. A point whose address is a parameter no code in the source passes
     * can't be followed further, and is left to a person, as local.
     */
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
        model.getTests().forEach((test, at) -> model.frameworkOf(test.type()).ifPresent(framework ->
            found.add(Facts.FrameworkTest.builder().test(TestChains.nameOf(test)).file(at.file()).line(at.line())
                .framework(framework.annotation()).annotatedOn(framework.on()).build())));
        return found;
    }

    private List<Facts.TestReach> testsReaching(final Map<String, Map<MethodKey, MethodKey>> reachedByKind,
                                                final Map<MethodKey, List<String>> systemsByTest) {
        final List<Facts.TestReach> tests = new ArrayList<>();
        model.getTests().forEach((test, at) -> {
            final Facts.TestReach.TestReachBuilder entry = Facts.TestReach.builder()
                .test(TestChains.nameOf(test)).file(at.file()).line(at.line())
                .systems(systemsByTest.getOrDefault(test, List.of()));
            boolean any = false;
            for (final Map.Entry<String, Map<MethodKey, MethodKey>> kind : reachedByKind.entrySet()) {
                final Optional<Facts.Reach> reach = reach(test, kind.getKey(), kind.getValue());
                reach.ifPresent(entry::reach);
                any |= reach.isPresent();
            }
            if (any) {
                tests.add(entry.build());
            }
        });
        return tests;
    }

    /** How a test reaches a kind of connection: through its own body first, otherwise through its set-up. */
    private Optional<Facts.Reach> reach(final MethodKey test, final String kind, final Map<MethodKey, MethodKey> next) {
        return TestChains.of(model, test, next).map(c -> reachOf(kind, c.steps(), c.throughSetUp()));
    }

    private static Facts.Reach reachOf(final String kind, final List<MethodKey> chain, final boolean throughSetUp) {
        return Facts.Reach.builder()
            .kind(kind)
            .point(chain.get(chain.size() - 1).toString())
            .throughSetUp(throughSetUp)
            .chain(chain.stream().map(MethodKey::toString).toList())
            .build();
    }
}
