package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.address.Locality;
import io.github.fiftieshousewife.connectionpaths.graph.CallGraph;
import io.github.fiftieshousewife.connectionpaths.graph.TestChains;
import io.github.fiftieshousewife.connectionpaths.settings.SettingsFiles;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import io.github.fiftieshousewife.connectionpaths.source.TestModel;
import io.github.fiftieshousewife.connectionpaths.source.TypeSolvers;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Turns the source model and call graph into {@link Facts}. */
public final class FactsBuilder {

    private static final int REASONS_REPORTED = 25;

    private final SourceModel model;
    private final CallGraph graph;
    private final SettingsFiles settings;
    private final Reaches reaches;
    private final PointAddresses pointAddresses;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "These are shared on purpose")
    public FactsBuilder(final SourceModel model, final CallGraph graph, final SettingsFiles settings) {
        this.model = model;
        this.graph = graph;
        this.settings = settings;
        this.reaches = new Reaches(model, settings);
        this.pointAddresses = new PointAddresses(model, graph);
    }

    /** The facts, naming the tool's version and what happened to the classpath's entries. */
    public Facts build(final String toolVersion, final TypeSolvers.Setup setup) {
        final List<Facts.Point> points = points();
        final Map<String, PointGroups.Group> groups = PointGroups.of(points);
        final List<TestFacts.TestReach> tests = testsReaching(ReachTargets.of(model, graph, groups),
            new TestFrameworks(model, setup.jupiter()));
        final List<Facts.FrameworkTest> frameworkTests = testsStartingAFramework();
        return Facts.builder()
            .toolVersion(toolVersion)
            .summary(summary(tests.size(), frameworkTests.size()))
            .connectionPoints(points)
            .systems(namedBy(new SystemsBuilder(model, graph).build(groups), tests))
            .tests(tests)
            .testsStartingAFramework(frameworkTests)
            .notResolved(model.getUnresolved().top(REASONS_REPORTED))
            .dispatchPointsNotFollowed(graph.getNotFollowed().entrySet().stream()
                .map(e -> new Facts.Dispatch(e.getKey(), e.getValue())).toList())
            .classpathNotes(setup.notes())
            .build();
    }

    // Each system with how many tests name it among their systems.
    private static List<Facts.ExternalSystem> namedBy(final List<Facts.ExternalSystem> systems,
                                                      final List<TestFacts.TestReach> tests) {
        final Map<String, Long> named = tests.stream().flatMap(test -> test.getSystems().stream())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return systems.stream().map(system -> system.toBuilder()
            .tests(named.getOrDefault(system.getName(), 0L).intValue()).build()).toList();
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
            .flatMap(p -> model.callsOf(p).stream().flatMap(call -> pointAddresses.of(p.method(), call.address())
                .stream().distinct().map(address -> point(p, call, settings.withValue(address)))))
            .toList();
    }

    // A call whose address comes from what its callers pass is listed once for each distinct address they pass.
    private Facts.Point point(final SourceModel.ConnectionPoint p, final SourceModel.PointCall call,
                              final Address address) {
        return Facts.Point.builder().kind(p.kind()).call(p.call()).method(p.method().toString())
            .file(call.at().file()).line(call.at().line()).address(address).local(localBecause(p, address)).build();
    }

    // Why a point stays on this machine, if it does: it listens, or its address is on this machine. An address that
    // can't be followed, such as a parameter no code in the source passes, is unknown, never local.
    private static String localBecause(final SourceModel.ConnectionPoint point, final Address address) {
        return Locality.ofTheCall(point.call()).or(() -> Locality.of(address, point.kind())).orElse(null);
    }

    private List<Facts.FrameworkTest> testsStartingAFramework() {
        final List<Facts.FrameworkTest> found = new ArrayList<>();
        model.tests().all().forEach((test, at) -> model.tests().frameworkOf(test.type()).ifPresent(framework ->
            found.add(Facts.FrameworkTest.builder().test(TestChains.nameOf(test)).file(at.file()).line(at.line())
                .framework(framework.annotation()).annotatedOn(framework.on()).build())));
        return found;
    }

    private List<TestFacts.TestReach> testsReaching(final List<ReachTargets.Target> targets,
                                                    final TestFrameworks frameworks) {
        final List<TestFacts.TestReach> tests = new ArrayList<>();
        model.tests().all().forEach((test, at) -> {
            final List<TestFacts.Reach> reaches = this.reaches.of(test, targets);
            if (!reaches.isEmpty()) {
                tests.add(entryOf(test, at, frameworks).reaches(reaches)
                    .systems(reaches.stream().map(TestFacts.Reach::getSystem).filter(Objects::nonNull).distinct()
                        .toList())
                    .build());
            }
        });
        return tests;
    }

    // A test that starts a framework loads the settings files on the test classpath, and its profiles' too, which
    // are not read yet.
    private TestFacts.TestReach.TestReachBuilder entryOf(final MethodKey test, final SourceModel.Location at,
                                                         final TestFrameworks frameworks) {
        final Optional<String> starts = model.tests().frameworkOf(test.type()).map(TestModel.Framework::annotation);
        return TestFacts.TestReach.builder()
            .test(TestChains.nameOf(test)).file(at.file()).line(at.line())
            .standIns(model.tests().standInsOf(test.type()))
            .startsFramework(starts.orElse(null))
            .testFramework(frameworks.of(at).orElse(null))
            .settingsFiles(starts.map(s -> settings.onTheTestClasspath().files()).orElse(null))
            .profiles(starts.map(s -> List.<String>of()).orElse(null));
    }
}
