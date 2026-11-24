package io.github.fiftieshousewife.connectionpaths;

import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.ORDERS_TEST;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.REACHING;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.onlyReach;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.reachOf;
import static org.assertj.core.api.Assertions.assertThat;

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
