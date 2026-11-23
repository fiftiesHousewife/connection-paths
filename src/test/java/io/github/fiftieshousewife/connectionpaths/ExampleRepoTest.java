package io.github.fiftieshousewife.connectionpaths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The example repo in src/test/resources/example has known answers; each test checks one of them. */
class ExampleRepoTest {

    private static final String ORDERS_TEST = "com.example.orders.OrderServiceTest";
    private static Facts facts;
    private static Map<String, Facts.TestReach> reaching;

    @BeforeAll
    static void readTheExample() {
        facts = ConnectionPaths.find(Options.builder()
            .mainSource(Path.of("src/test/resources/example/src/main/java"))
            .testSource(Path.of("src/test/resources/example/src/test/java"))
            .build());
        reaching = facts.getTests().stream().collect(Collectors.toMap(Facts.TestReach::getTest, t -> t));
    }

    @Test
    void findsEachConnectionPointWithItsKind() {
        assertThat(facts.getConnectionPoints())
            .extracting(Facts.Point::getMethod, Facts.Point::getKind)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("com.example.db.Database#open()", "database"),
                org.assertj.core.groups.Tuple.tuple("com.example.events.AuditListener#changed(String)", "socket"),
                org.assertj.core.groups.Tuple.tuple("com.example.report.ReportClient#fetch()", "url"),
                org.assertj.core.groups.Tuple.tuple("com.example.tools.Shell#list()", "process"),
                org.assertj.core.groups.Tuple.tuple("com.example.docs.DocumentReader#read(URL)", "url"));
    }

    @Test
    void listsOnlyTheTestsThatMayReachAConnection() {
        assertThat(reaching.keySet()).containsExactlyInAnyOrder(
            ORDERS_TEST + ".countsOrders",
            "com.example.report.ReportJobTest.runsTheReport",
            "com.example.ledger.LedgerTest.countsEntries",
            "com.example.tools.ShellTest.lists",
            "com.example.legacy.AuditTest.testAudits",
            "com.example.orders.ArchiverTest.archives");
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
            () -> assertThat(facts.getDispatchPointsNotFollowed())
                .containsExactly(new Facts.Dispatch("com.example.events.Listener#changed(String)", 4)),
            () -> assertThat(reaching).doesNotContainKey("com.example.events.EventsTest.firesToEveryListener"));
    }

    @Test
    void tellsOverloadsApartByTheirParameterTypes() {
        assertThat(reaching).doesNotContainKey("com.example.docs.DocumentReaderTest.readsText");
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
        assertThat(reaching).doesNotContainKey("com.example.util.NamesTest.joinsNames");
    }

    @Test
    void reportsCallsItCannotResolveByReason() {
        assertAll(
            () -> assertThat(facts.getSummary().getCallsNotResolved()).isPositive(),
            () -> assertThat(facts.getNotResolved()).extracting(Unresolved.Reason::reason)
                .anyMatch(reason -> reason.contains("Post")));
    }

    @Test
    void summarisesWhatItRead() {
        final Facts.Summary summary = facts.getSummary();
        assertAll(
            () -> assertThat(summary.getFiles()).isEqualTo(31),
            () -> assertThat(summary.getFilesNotParsed()).isZero(),
            () -> assertThat(summary.getTests()).isEqualTo(10),
            () -> assertThat(summary.getTestsReaching()).isEqualTo(6));
    }

    private static Facts.Reach onlyReach(final String test) {
        final List<Facts.Reach> reaches = reaching.get(test).getReaches();
        assertThat(reaches).hasSize(1);
        return reaches.get(0);
    }
}
