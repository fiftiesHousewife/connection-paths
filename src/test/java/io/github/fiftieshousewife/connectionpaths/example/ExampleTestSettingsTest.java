package io.github.fiftieshousewife.connectionpaths.example;

import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.ORDERS_TEST;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.REACHING;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.onlyReach;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.reachOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.fiftieshousewife.connectionpaths.facts.TestFacts;
import org.junit.jupiter.api.Test;

// Where the example repo's tests would connect, from the settings on the test classpath: its test resources first,
// then its main ones.
class ExampleTestSettingsTest {

    @Test
    void takesASettingFromTheTestResourcesBeforeTheMainOnes() {
        assertThat(reachOf("com.example.addresses.CrmClientTest.opens", "socket").getAddressForTests())
            .isEqualTo("local");
    }

    @Test
    void saysNoneWhereNoSettingOnTheTestClasspathGivesTheAddress() {
        assertThat(reachOf("com.example.addresses.AuditFeedTest.readsTheFeed", "url").getAddressForTests())
            .isEqualTo("none");
    }

    @Test
    void givesWhatRunsOnAnOpenDatabaseTheDatabasesAddressForTests() {
        assertThat(reachOf("com.example.schema.SchemaUpgradeTest.upgrades", "host-lookup").getAddressForTests())
            .isEqualTo("none");
    }

    @Test
    void givesACallPassedARowsValueTheDatabasesAddressForTests() {
        assertThat(reachOf("com.example.schema.SchemaOwnersTest.findsTheFirstOwner", "host-lookup")
            .getAddressForTests()).isEqualTo("none");
    }

    @Test
    void sendsARelativePathToTheAppATestServesOnAPort() {
        assertThat(reachOf("com.example.boot.OrdersOverHttpTest.showsAnOrder", "http").getAddressForTests())
            .isEqualTo("local");
    }

    @Test
    void takesTheAddressATestHandsABuilderWhereTheChainLeavesItUnknown() {
        assertThat(reachOf("com.example.pages.PageRequestTest.fetchesAPage", "url").getAddressForTests())
            .isEqualTo("host");
    }

    @Test
    void takesTheAddressTheSetUpHandsABuilder() {
        assertThat(reachOf("com.example.pages.PageInSetUpTest.hasAPage", "url").getAddressForTests())
            .isEqualTo("host");
    }

    @Test
    void takesTheHostTheSetUpConnectsToWhenTheTestsOwnConnectionIsKnownOnlyAtRunTime() {
        assertThat(reachOf("com.example.transfers.FileServerClientTest.listsTheFiles", "socket"))
            .extracting(TestFacts.Reach::getAddressForTests, TestFacts.Reach::isThroughSetUp)
            .containsExactly("host", true);
    }

    @Test
    void takesANumericAddressMadeFromItsBytesAsWhereItSays() {
        assertThat(reachOf("com.example.lookups.TimeClientTest.asksAServerAtANumericAddress", "socket")
            .getAddressForTests()).isEqualTo("local");
    }

    @Test
    void takesTheLoopbackAddressAsThisMachine() {
        assertThat(reachOf("com.example.lookups.TimeClientTest.asksTheLoopbackAddress", "socket")
            .getAddressForTests()).isEqualTo("local");
    }

    @Test
    void takesAnAddressOnThisMachineAsLocalWhateverIsAddedToIt() {
        assertThat(java.util.stream.Stream.of("fetchesTheFirstPage", "fetchesAPageWithAQuery", "fetchesAPageByItsPath",
                "fetchesEachPageInAList", "fetchesEachPageByItsPlaceInAList")
            .map(test -> reachOf("com.example.pages.LocalPagesTest." + test, "url").getAddressForTests()))
            .containsExactly("local", "local", "local", "local", "local");
    }

    @Test
    void takesASettingsDefaultWhereNoFileSetsItAndAnEnvironmentVariableAsKnownOnlyAtRunTime() {
        assertThat(java.util.stream.Stream.of("readsPrices", "readsStock", "readsPayments", "readsRates")
            .map(test -> reachOf("com.example.addresses.PriceFeedsTest." + test, "url").getAddressForTests()))
            .containsExactly("host", "run-time", "local", "none");
    }

    @Test
    void takesTheArgumentOfTheCallIntoTheChainNotOfAnotherCallOfTheSameName() {
        assertThat(reachOf("com.example.pages.PageClientTest.fetchesThePageItIsGiven", "url").getAddressForTests())
            .isEqualTo("local");
    }

    @Test
    void takesAnInternationalHostNameAsAHost() {
        assertThat(reachOf("com.example.pages.PageRequestTest.fetchesAPageWithAnInternationalName", "url")
            .getAddressForTests()).isEqualTo("host");
    }

    @Test
    void takesASettingOnlyTheMainResourcesGiveAsTheTestsToo() {
        assertThat(onlyReach("com.example.clients.ShopTest.countsOrders").getAddressForTests()).isEqualTo("host");
    }

    @Test
    void givesALiteralsHost() {
        assertThat(onlyReach(ORDERS_TEST + ".countsOrders").getAddressForTests()).isEqualTo("host");
    }

    @Test
    void followsAParameterBackToTheAddressTheTestItselfPasses() {
        assertThat(onlyReach("com.example.overloads.LoaderTest.loadsFromAUrl").getAddressForTests())
            .isEqualTo("host");
    }

    @Test
    void saysRunTimeForAnAddressTheSourceDoesNotGive() {
        assertThat(onlyReach("com.example.clients.ShopTest.listsPrices").getAddressForTests()).isEqualTo("run-time");
    }

    @Test
    void namesTheEmbeddedServerATestStartsAsItsStandIn() {
        assertThat(REACHING.get("com.example.mail.MailLoginWithGreenMailTest.checksAUser").getStandIns())
            .containsExactly("com.icegreen.greenmail.junit5.GreenMailExtension");
    }

    @Test
    void namesNoStandInForATestThatStartsNone() {
        assertThat(REACHING.get("com.example.mail.MailLoginTest.checksAUser").getStandIns()).isEmpty();
    }

    @Test
    void followsAParameterBackToTheArgumentItsCallerPassesAndThroughAnEnvironmentTable() {
        assertThat(onlyReach("com.example.directory.LdapConnectorTest.connects").getAddressForTests())
            .isEqualTo("none");
    }
}
