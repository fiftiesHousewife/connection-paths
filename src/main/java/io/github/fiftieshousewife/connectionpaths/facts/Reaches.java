package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.graph.TestChains;
import io.github.fiftieshousewife.connectionpaths.settings.SettingsFiles;
import io.github.fiftieshousewife.connectionpaths.source.Mark;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

// How a test reaches each kind of connection: one chain of calls, through its own body first, otherwise through its
// set-up; whether it runs a mock on the way; and where the test's own settings send it. A chain through code given
// an open database connection runs only once the test's database reach connects, so where that can't, neither can
// this: it takes the database reach's address for tests, and its mock.
final class Reaches {

    private static final String DATABASE = "database";

    private final SourceModel model;
    private final MockedChains mocks;
    private final TestAddresses testAddresses;
    private final TestCalls testCalls;
    private final RunsFor runs;

    Reaches(final SourceModel model, final SettingsFiles settings) {
        this.model = model;
        this.mocks = new MockedChains(model);
        this.testAddresses = new TestAddresses(model, settings);
        this.testCalls = new TestCalls(model);
        this.runs = new RunsFor(model);
    }

    List<TestFacts.Reach> of(final MethodKey test, final List<ReachTargets.Target> targets) {
        final List<TestFacts.Reach> reaches = targets.stream()
            .map(target -> reach(test, target.kind(), target.next())
                .map(reach -> reach.toBuilder().system(target.system()).build()))
            .flatMap(Optional::stream).filter(reach -> runsFor(test, reach)).toList();
        final List<String> standIns = List.copyOf(model.tests().standInsOf(test.type()));
        return behindTheDatabase(reaches).stream().map(reach -> graded(reach, standIns)).toList();
    }

    // Where every database reach can't connect, the code that runs only on an open database can't either.
    private List<TestFacts.Reach> behindTheDatabase(final List<TestFacts.Reach> reaches) {
        final List<TestFacts.Reach> databases = reaches.stream().filter(r -> DATABASE.equals(r.getKind())).toList();
        return databases.isEmpty() || !databases.stream().allMatch(Reaches::cannotConnect) ? reaches
            : reaches.stream().map(r -> behind(r, databases.get(0))).toList();
    }

    private static TestFacts.Reach graded(final TestFacts.Reach reach, final List<String> standIns) {
        final Grades.Grade grade = Grades.of(reach, standIns);
        return reach.toBuilder().grade(grade.grade()).reason(grade.reason()).reasonText(grade.text()).build();
    }

    // Through the test's own body, unless that goes to an address known only at run time and its set-up reaches the
    // same kind of connection at a host or on this machine: a client the set-up connects, then the test uses.
    private Optional<TestFacts.Reach> reach(final MethodKey test, final String kind,
                                            final Map<MethodKey, MethodKey> next) {
        final Optional<TestFacts.Reach> first = TestChains.of(model, test, next).map(c -> reachOf(test, kind, c));
        final boolean runTime = first.filter(r -> TestAddresses.RUN_TIME.equals(r.getAddressForTests())).isPresent();
        return runTime ? Optional.of(TestChains.throughSetUp(model, test, next).map(c -> reachOf(test, kind, c))
            .filter(r -> isKnown(r) && runsFor(test, r)).orElse(first.get())) : first;
    }

    private static boolean isKnown(final TestFacts.Reach reach) {
        return TestAddresses.HOST.equals(reach.getAddressForTests())
            || TestAddresses.LOCAL.equals(reach.getAddressForTests());
    }

    private TestFacts.Reach reachOf(final MethodKey test, final String kind, final TestChains.TestChain chain) {
        final List<MethodKey> steps = chain.steps();
        final TestAddresses.ForTests forTests = testAddresses.of(steps, kind);
        final Optional<TestCalls.FirstCall> first = testCalls.of(steps, chain.throughSetUp());
        return TestFacts.Reach.builder()
            .kind(kind)
            .point(steps.get(steps.size() - 1).toString())
            .throughSetUp(chain.throughSetUp())
            .throughMock(mocks.throughMock(test, steps).map(MethodKey::toString).orElse(null))
            .addressForTests(forTests.grade())
            .host(forTests.host())
            .firstCall(first.map(TestCalls.FirstCall::at).orElse(null))
            .clientField(first.map(TestCalls.FirstCall::field).orElse(null))
            .chain(steps.stream().map(MethodKey::toString).toList())
            .build();
    }

    private boolean runsFor(final MethodKey test, final TestFacts.Reach reach) {
        return runs.test(test, reach.getChain());
    }

    private static boolean cannotConnect(final TestFacts.Reach database) {
        return database.getThroughMock() != null || TestAddresses.NONE.equals(database.getAddressForTests());
    }

    private TestFacts.Reach behind(final TestFacts.Reach reach, final TestFacts.Reach database) {
        final List<String> chain = reach.getChain();
        final Set<String> open = model.marked(Mark.ON_AN_OPEN_DATABASE);
        final boolean needsIt = !DATABASE.equals(reach.getKind()) && IntStream.range(0, chain.size())
            .anyMatch(i -> open.contains(chain.get(i))
                || i > 0 && open.contains(chain.get(i - 1) + " > " + chain.get(i)));
        return needsIt ? reach.toBuilder().addressForTests(database.getAddressForTests()).host(database.getHost())
            .throughMock(Optional.ofNullable(reach.getThroughMock()).orElse(database.getThroughMock())).build()
            : reach;
    }
}
