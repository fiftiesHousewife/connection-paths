package io.github.fiftieshousewife.connectionpaths;

import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.CONFIG_TEST;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.DB_MANAGER_TEST;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.FACTS;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.ORDERS_TEST;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.REACHING;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.STORES_TEST;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.onlyReach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

/** The example repo in src/test/resources/example has known answers; each test checks one of them. */
class ExampleRepoTest {

    @Test
    void findsEachConnectionPointWithItsKind() {
        assertThat(FACTS.getConnectionPoints())
            .extracting(Facts.Point::getMethod, Facts.Point::getKind)
            .containsExactlyInAnyOrder(
                tuple("com.example.db.Database#open()", "database"),
                tuple("com.example.events.AuditListener#changed(String)", "socket"),
                tuple("com.example.report.ReportClient#fetch()", "url"),
                tuple("com.example.tools.Shell#list()", "process"),
                tuple("com.example.docs.DocumentReader#read(URL)", "url"),
                tuple("com.example.mail.SmtpMailer#send(String)", "mail"),
                tuple("com.example.mail.SmtpMailer#send(String)", "mail"),
                tuple("com.example.mail.MailLogin#check(String,String)", "mail"),
                tuple("com.example.addresses.CrmClient#open()", "socket"),
                tuple("com.example.addresses.LedgerStore#open()", "database"),
                tuple("com.example.addresses.OrdersLookup#connect(InitialContext)",
                    "database"),
                tuple("com.example.addresses.PaymentsClient#balances()", "url"),
                tuple("com.example.addresses.PoolFactory#connect(SimpleDataSource)",
                    "database"),
                tuple("com.example.addresses.RegionClient#prices(String)", "url"),
                tuple("com.example.addresses.WarehouseClient#stock()", "url"),
                tuple("com.example.alt.DefaultProvider#getConnection()", "database"),
                tuple("com.example.alt.JndiProvider#getConnection()", "database"),
                tuple("com.example.alt.EmbeddedProvider#getConnection()", "database"),
                tuple("com.example.alt.LocalThings#readBundledFile()", "url"),
                tuple("com.example.alt.LocalThings#talkToTheAgentOnThisMachine()",
                    "socket"),
                tuple("com.example.alt.LocalThings#readOwnJar()", "url"),
                tuple("com.example.addresses.DirectoryClient#users()", "http"),
                tuple("com.example.addresses.Notifier.Sender#open()", "socket"),
                tuple("com.example.addresses.SecureChannel#open()", "socket"),
                tuple("com.example.addresses.Fetcher#fetch()", "url"),
                tuple("com.example.overloads.RemoteLoader#load(URL)", "url"),
                tuple("com.example.jdbc.Customers#count()", "database"),
                tuple("com.example.jdbc.Customers#named(String)", "database"),
                tuple("com.example.clients.Shop#orderCount()", "http"),
                tuple("com.example.clients.Shop#priceList()", "http"),
                tuple("com.example.addresses.ListenAddress#toListenOn()", "host-lookup"),
                tuple("com.example.addresses.ListenAddress#partner()", "host-lookup"),
                tuple("com.example.addresses.OrdersReplica#open()", "database"));
    }

    @Test
    void listsOnlyTheTestsThatMayReachAConnection() {
        assertThat(REACHING.keySet()).containsExactlyInAnyOrder(
            ORDERS_TEST + ".countsOrders",
            "com.example.report.ReportJobTest.runsTheReport",
            "com.example.ledger.LedgerTest.countsEntries",
            "com.example.tools.ShellTest.lists",
            "com.example.legacy.AuditTest.testAudits",
            "com.example.orders.ArchiverTest.archives",
            STORES_TEST + ".namesTheStore",
            STORES_TEST + ".sizesTheCache",
            CONFIG_TEST + ".readsASetting",
            CONFIG_TEST + ".sizesThePool",
            "com.example.greet.GreetingsTest.says",
            "com.example.ledger.LedgerNestedTest.WhenEmpty.hasNoEntries",
            "com.example.ledger.LedgerTestNgTest.countsEntries",
            "com.example.ledger.LedgerCleanupTest.startsClean",
            "com.example.mail.SmtpMailerTest.sends",
            "com.example.mail.MailLoginTest.checksAUser",
            DB_MANAGER_TEST,
            "com.example.overloads.LoaderTest.loadsFromAUrl",
            "com.example.jdbc.CustomersTest.counts",
            "com.example.jdbc.CustomersTest.findsByName",
            "com.example.clients.ShopTest.countsOrders",
            "com.example.clients.ShopTest.listsPrices",
            "com.example.inherited.LedgerResourceTest.isReady");
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
            () -> assertThat(summary.getFiles()).isEqualTo(104),
            () -> assertThat(summary.getFilesNotParsed()).isZero(),
            // The abstract ResourceContractTest's one test is counted twice, under each subclass, not under itself.
            () -> assertThat(summary.getTests()).isEqualTo(37),
            () -> assertThat(summary.getTestsReaching()).isEqualTo(23));
    }
}
