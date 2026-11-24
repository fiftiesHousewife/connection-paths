package io.github.fiftieshousewife.connectionpaths;

import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import io.github.fiftieshousewife.connectionpaths.source.Unresolved;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.FACTS;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.ORDERS_TEST;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.REACHING;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.onlyReach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

/** The example repo in src/test/resources/example has known answers; each test checks one of them. */
class ExampleRepoTest {

    @Test
    void findsEachConnectionPointWithItsKind() {
        assertThat(FACTS.getConnectionPoints()).extracting(p -> p.getMethod() + " " + p.getKind())
            .containsExactlyInAnyOrderElementsOf(ExampleRepo.expected("connection-points.txt"));
    }

    @Test
    void listsOnlyTheTestsThatMayReachAConnection() {
        assertThat(REACHING.keySet()).containsExactlyInAnyOrderElementsOf(ExampleRepo.expected("tests-reaching.txt"));
    }

    @Test
    void followsACallThroughAnInterfaceWithFewImplementations() {
        assertThat(onlyReach(ORDERS_TEST + ".countsOrders").getChain()).containsExactly(
            ORDERS_TEST + "#countsOrders()",
            "com.example.orders.OrderService#total()",
            "com.example.orders.JdbcOrderStore#all()",
            "com.example.db.Database#openQuietly()",
            "com.example.db.Database#open()");
    }

    @Test
    void countsCallsInsideALambdaAsTheEnclosingMethods() {
        assertThat(onlyReach("com.example.report.ReportJobTest.runsTheReport").getChain()).containsExactly(
            "com.example.report.ReportJobTest#runsTheReport()",
            "com.example.report.ReportJob#run()",
            "com.example.report.ReportClient#fetch()");
    }

    @Test
    void findsAConnectionReachedThroughSetUpAndFieldInitialisers() {
        final Facts.Reach reach = onlyReach("com.example.ledger.LedgerTest.countsEntries");
        assertAll(
            () -> assertThat(reach.isThroughSetUp()).isTrue(),
            () -> assertThat(reach.getChain()).containsExactly(
                "com.example.ledger.LedgerTest#countsEntries()",
                "com.example.ledger.LedgerTest#createLedger()",
                "com.example.ledger.Ledger#<fields>()",
                "com.example.db.Database#openQuietly()",
                "com.example.db.Database#open()"));
    }

    @Test
    void recognisesJunit3TestsByTheirSuperclass() {
        assertThat(onlyReach("com.example.legacy.AuditTest.testAudits").getKind()).isEqualTo("socket");
    }

    @Test
    void reportsADispatchPointWithManyImplementationsInsteadOfFollowingIt() {
        assertAll(
            () -> assertThat(FACTS.getDispatchPointsNotFollowed())
                .containsExactly(new Facts.Dispatch("com.example.events.Listener#changed(String)", 4)),
            () -> assertThat(REACHING).doesNotContainKey("com.example.events.EventsTest.firesToEveryListener"));
    }

    @Test
    void tellsOverloadsApartByTheirParameterTypes() {
        assertThat(REACHING).doesNotContainKey("com.example.docs.DocumentReaderTest.readsText");
    }

    @Test
    void followsAGenericInterfaceToAnImplementationNamingItsTypeArgument() {
        assertThat(onlyReach("com.example.orders.ArchiverTest.archives").getChain()).containsExactly(
            "com.example.orders.ArchiverTest#archives()",
            "com.example.orders.Archiver#archive(String)",
            "com.example.orders.OrderArchive#save(String)",
            "com.example.db.Database#openQuietly()",
            "com.example.db.Database#open()");
    }

    @Test
    void doesNotFollowCallsThroughTheJdksOwnTypes() {
        assertThat(REACHING).doesNotContainKey("com.example.util.NamesTest.joinsNames");
    }

    @Test
    void reportsCallsItCannotResolveByReason() {
        assertAll(
            () -> assertThat(FACTS.getSummary().getCallsNotResolved()).isPositive(),
            () -> assertThat(FACTS.getNotResolved()).extracting(Unresolved.Reason::reason)
                .anyMatch(reason -> reason.contains("Post")));
    }

    @Test
    void summarisesWhatItRead() {
        final Facts.Summary summary = FACTS.getSummary();
        assertAll(
            () -> assertThat(summary.getFiles()).isEqualTo(166),
            () -> assertThat(summary.getFilesNotParsed()).isZero(),
            // The abstract ResourceContractTest's one test is counted twice, under each subclass, not under itself.
            () -> assertThat(summary.getTests()).isEqualTo(61),
            () -> assertThat(summary.getTestsReaching()).isEqualTo(45));
    }
}
