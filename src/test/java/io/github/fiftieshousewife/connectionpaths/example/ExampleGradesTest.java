package io.github.fiftieshousewife.connectionpaths.example;

import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.REACHING;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.onlyReach;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.reachOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.fiftieshousewife.connectionpaths.facts.TestFacts;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

// Whether each reach connects, and the reason, by the first rule that holds; and what a repair of a test needs: where
// its chain starts, the field it calls, its test framework, and the settings files a framework test loads.
class ExampleGradesTest {

    private static final String PRICES = "com.example.addresses.PriceFeedsTest.readsPrices";
    private static final String CONTAINER = "com.example.boot.LedgerContainerTest.storesAnEntry";

    @Test
    void gradesEachReachByTheFirstRuleThatHolds() {
        assertThat(Stream.of(
                reachOf("com.example.mocked.StoreWithMockFieldTest.totalsTheOrders", "database"),
                reachOf(CONTAINER, "database"),
                reachOf("com.example.report.ReportClientWithServerTest.fetches", "url"),
                reachOf("com.example.addresses.AuditFeedTest.readsTheFeed", "url"),
                reachOf("com.example.addresses.CrmClientTest.opens", "socket"),
                reachOf(PRICES, "url"),
                reachOf("com.example.addresses.PriceFeedsTest.readsStock", "url")))
            .extracting(TestFacts.Reach::getGrade, TestFacts.Reach::getReason)
            .containsExactly(
                tuple("cannot-reach", "through-a-mock"),
                tuple("cannot-reach", "embedded-server"),
                tuple("cannot-reach", "embedded-server"),
                tuple("cannot-reach", "no-address-for-tests"),
                tuple("cannot-reach", "local"),
                tuple("reaches", "host"),
                tuple("may-reach", "run-time"));
    }

    @Test
    void gradesAServerListeningOnThisMachineAsLocalAndPartOfNoSystem() {
        assertThat(onlyReach("com.example.transfers.LocalFileServerTest.startsAndStops"))
            .extracting(TestFacts.Reach::getSystem, TestFacts.Reach::getGrade, TestFacts.Reach::getReason)
            .containsExactly(null, "cannot-reach", "local");
    }

    // A client the test makes with no address, and calls only to connect, has none to connect to.
    @Test
    void givesNoAddressToAClientTheTestMakesAndNeverGivesOne() {
        assertThat(Stream.of("com.example.fetch.ReportFetcherTest.refusesWithoutAUrl",
                "com.example.fetch.ReportFetcherTest.fetchesTheReport").map(test -> reachOf(test, "url")))
            .extracting(TestFacts.Reach::getAddressForTests).containsExactly("none", "host");
    }

    @Test
    void givesTheHostWhereTheChainStartsAndTheTestClassFieldItCalls() {
        assertThat(reachOf(PRICES, "url"))
            .extracting(TestFacts.Reach::getSystem, TestFacts.Reach::getHost, r -> r.getFirstCall().getLine(),
                TestFacts.Reach::getClientField)
            .containsExactly("pricing.internal", "pricing.internal", 11, "feeds");
    }

    @Test
    void startsAChainThroughASetUpAtTheSetUpsCall() {
        assertThat(reachOf("com.example.ledger.LedgerTest.countsEntries", "database").getFirstCall().getLine())
            .isEqualTo(13);
    }

    @Test
    void namesEachTestsTestFramework() {
        assertThat(Stream.of("com.example.legacy.AuditTest.testAudits",
                "com.example.orders.OrderServiceTest.countsOrders", PRICES,
                "com.example.ledger.LedgerTestNgTest.countsEntries")
            .map(test -> REACHING.get(test).getTestFramework()))
            .containsExactly("junit3", "junit4", "junit5", "testng");
    }

    @Test
    void listsTheSettingsFilesAndProfilesATestStartingAFrameworkLoads() {
        assertThat(REACHING.get(CONTAINER))
            .extracting(TestFacts.TestReach::getStartsFramework,
                test -> test.getSettingsFiles().stream().map(file -> file.substring(file.indexOf("/src/") + 1))
                    .toList(),
                TestFacts.TestReach::getProfiles)
            .containsExactly("@Testcontainers", List.of("src/main/resources/application.properties",
                "src/main/resources/application.yml", "src/main/resources/applicationContext.xml",
                "src/main/resources/config.xml", "src/test/resources/application.properties"), List.of());
    }

    // Only the dev profile's file sets rates.url: a test that activates the profile connects to the host it gives.
    @Test
    void takesTheSettingsOfTheProfilesATestActivatesBeforeThePlainOnes() {
        final TestFacts.TestReach test = REACHING.get("com.example.addresses.DevPriceFeedsTest.readsTheDevRates");
        assertThat(List.of(test.getReaches().get(0).getHost(), test.getProfiles(),
                test.getSettingsFiles().stream().filter(file -> file.contains("-dev")).toList()))
            .extracting(Object::toString)
            .containsExactly("rates-dev.internal", "[dev]",
                "[src/test/resources/example/src/main/resources/application-dev.properties]");
    }
}
