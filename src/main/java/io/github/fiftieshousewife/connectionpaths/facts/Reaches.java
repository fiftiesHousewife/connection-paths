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
    private final RunsFor runs;

    Reaches(final SourceModel model, final SettingsFiles settings) {
        this.model = model;
        this.mocks = new MockedChains(model);
        this.testAddresses = new TestAddresses(model, settings);
        this.runs = new RunsFor(model);
    }

    List<Facts.Reach> of(final MethodKey test, final Map<String, Map<MethodKey, MethodKey>> reachedByKind) {
        final List<Facts.Reach> reaches = reachedByKind.entrySet().stream()
            .map(kind -> reach(test, kind.getKey(), kind.getValue()))
            .flatMap(Optional::stream).filter(reach -> runsFor(test, reach)).toList();
        return reaches.stream().filter(r -> DATABASE.equals(r.getKind())).findFirst()
            .filter(Reaches::cannotConnect)
            .map(database -> reaches.stream().map(r -> behind(r, database)).toList())
            .orElse(reaches);
    }

    // Through the test's own body, unless that goes to an address known only at run time and its set-up reaches the
    // same kind of connection at a host or on this machine: a client the set-up connects, then the test uses.
    private Optional<Facts.Reach> reach(final MethodKey test, final String kind, final Map<MethodKey, MethodKey> next) {
        final Optional<Facts.Reach> first = TestChains.of(model, test, next).map(c -> reachOf(test, kind, c));
        final boolean runTime = first.filter(r -> TestAddresses.RUN_TIME.equals(r.getAddressForTests())).isPresent();
        return runTime ? Optional.of(TestChains.throughSetUp(model, test, next).map(c -> reachOf(test, kind, c))
            .filter(r -> isKnown(r) && runsFor(test, r)).orElse(first.get())) : first;
    }

    private static boolean isKnown(final Facts.Reach reach) {
        return TestAddresses.HOST.equals(reach.getAddressForTests())
            || TestAddresses.LOCAL.equals(reach.getAddressForTests());
    }

    private Facts.Reach reachOf(final MethodKey test, final String kind, final TestChains.TestChain chain) {
        final List<MethodKey> steps = chain.steps();
        return Facts.Reach.builder()
            .kind(kind)
            .point(steps.get(steps.size() - 1).toString())
            .throughSetUp(chain.throughSetUp())
            .throughMock(mocks.throughMock(test, steps).map(MethodKey::toString).orElse(null))
            .addressForTests(testAddresses.of(steps, kind))
            .chain(steps.stream().map(MethodKey::toString).toList())
            .build();
    }

    private boolean runsFor(final MethodKey test, final Facts.Reach reach) {
        return runs.test(test, reach.getChain());
    }

    private static boolean cannotConnect(final Facts.Reach database) {
        return database.getThroughMock() != null || TestAddresses.NONE.equals(database.getAddressForTests());
    }

    private Facts.Reach behind(final Facts.Reach reach, final Facts.Reach database) {
        final List<String> chain = reach.getChain();
        final Set<String> open = model.marked(Mark.ON_AN_OPEN_DATABASE);
        final boolean needsIt = !DATABASE.equals(reach.getKind()) && IntStream.range(0, chain.size())
            .anyMatch(i -> open.contains(chain.get(i))
                || i > 0 && open.contains(chain.get(i - 1) + " > " + chain.get(i)));
        return needsIt ? reach.toBuilder().addressForTests(database.getAddressForTests())
            .throughMock(Optional.ofNullable(reach.getThroughMock()).orElse(database.getThroughMock())).build()
            : reach;
    }
}
