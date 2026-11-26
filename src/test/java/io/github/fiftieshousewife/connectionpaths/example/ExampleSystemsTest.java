package io.github.fiftieshousewife.connectionpaths.example;

import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import io.github.fiftieshousewife.connectionpaths.facts.TestFacts;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.DB_MANAGER_TEST;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.FACTS;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.OPEN;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.OPEN_QUIETLY;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.REACHING;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.onlyReach;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.system;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** How the example repo's connection points are grouped into the systems tests may reach. */
class ExampleSystemsTest {

    @Test
    void givesATestOneReachForEachSystemItReaches() {
        assertThat(REACHING.get(DB_MANAGER_TEST).getReaches())
            .extracting(TestFacts.Reach::getKind, TestFacts.Reach::getSystem, TestFacts.Reach::getPoint)
            .containsExactly(
                tuple("database", null, "com.example.alt.EmbeddedProvider#getConnection()"),
                tuple("database", "java:comp/env/jdbc/app", "com.example.alt.JndiProvider#getConnection()"),
                tuple("database", "main-db.internal", "com.example.alt.DefaultProvider#getConnection()"));
    }

    // A client whose code is in the source opens its connection there: a test's call to it is no point, and the host
    // the test gives it is a system of the client's own point.
    @Test
    void takesTheConnectionAClientInTheSourceOpensForItsPointNotTheCallToIt() {
        final TestFacts.Reach reach = onlyReach("org.apache.commons.net.SocketClientTest.connectsToTheMirror");
        assertThat(List.of(reach.getSystem(), reach.getPoint(), reach.getClientField(),
                system("ftp-mirror.internal").getPoints()))
            .containsExactly("ftp-mirror.internal", "org.apache.commons.net.SocketClient#connect(String,int)", "client",
                List.of("org.apache.commons.net.SocketClient#connect(String,int)"));
    }

    // A host known only at run time is the client's own system, of its class: no reach that may connect is left out.
    @Test
    void givesAReachToAHostKnownOnlyAtRunTimeTheSystemOfItsPointsClass() {
        assertThat(onlyReach("com.example.lines.LineClientTest.connectsToAHostItIsHanded").getSystem())
            .isEqualTo("com.example.lines.LineClient");
    }

    // The host goes through an overload whose parameter has the same name as the one it calls.
    @Test
    void followsATestsHostThroughAnOverloadToTheSystemOfTheClientsPoint() {
        assertThat(onlyReach("org.apache.commons.net.SocketClientTest.connectsOnTheDefaultPort").getSystem())
            .isEqualTo("ftp-default.internal");
    }

    @Test
    void putsAPointInTheSystemOfEachHostItsCallersAndTestsPass() {
        assertThat(FACTS.getSystems())
            .filteredOn(system -> system.getPoints().contains("com.example.lines.LineClient#connect(String)"))
            .extracting(Facts.ExternalSystem::getName)
            .containsExactlyInAnyOrder("com.example.lines.LineClient", "line.host", "lines.internal", "news.internal",
                "weather.internal");
    }

    // A client every caller gives its own host, tests among them, is in each host's system; a test reaches only the
    // one its own chain passes.
    @Test
    void namesForATestOnlyTheSystemItsOwnChainGivesTheAddressOf() {
        assertThat(Stream.of("com.example.lines.WeatherLineTest.readsTheWeather",
                "com.example.lines.LineClientTest.connectsToItsOwnHost",
                "com.example.lines.LineClientTest.connectsToAHostKnownOnlyAtRunTime")
            .map(test -> REACHING.get(test).getReaches()).toList())
            .extracting(reaches -> reaches.stream().map(r -> r.getSystem() + " " + r.getHost()).toList())
            .containsExactly(List.of("weather.internal weather.internal"), List.of("lines.internal lines.internal"),
                List.of("line.host null"));
    }

    // The test's own socket to lines.internal is the reach to that system; its call into the client goes to the same
    // host, so it is no reach of its own without a system.
    @Test
    void leavesOutAReachWithNoSystemToAHostTheTestAlreadyReachesItsSystemOf() {
        assertThat(REACHING.get("com.example.lines.LineCheckTest.checksTheLineThenConnects").getReaches())
            .extracting(TestFacts.Reach::getKind, TestFacts.Reach::getSystem, TestFacts.Reach::getPoint)
            .containsExactly(tuple("socket", "lines.internal",
                "com.example.lines.LineCheckTest#checksTheLineThenConnects()"));
    }

    @Test
    void groupsConnectionPointsIntoSystemsByHostThenNameThenClass() {
        assertThat(FACTS.getSystems()).extracting(Facts.ExternalSystem::getName, Facts.ExternalSystem::getNamedBy)
            .contains(
                tuple("orders-db.internal", "host"),
                tuple("crm.internal", "host"),
                tuple("warehouse.url", "setting"),
                tuple("PAYMENTS_URL", "environment-variable"),
                tuple("java:comp/env/jdbc/orders", "jndi-name"),
                tuple("com.example.addresses.RegionClient", "class"));
    }

    @Test
    void putsAPointWithNoAddressOfItsOwnInTheSystemItsMethodConnectsTo() {
        assertThat(system("mail.internal").getPoints()).containsExactly("com.example.mail.SmtpMailer#send(String)");
    }

    @Test
    void marksSystemsReachedThroughTheSameInterfaceMethodAsAlternatives() {
        assertAll(
            () -> assertThat(system("main-db.internal").getAlternatives()).containsExactly("java:comp/env/jdbc/app"),
            () -> assertThat(system("java:comp/env/jdbc/app").getAlternatives()).containsExactly("main-db.internal"),
            () -> assertThat(REACHING.get(DB_MANAGER_TEST).getSystems())
                .containsExactly("java:comp/env/jdbc/app", "main-db.internal"));
    }

    @Test
    void givesEachSystemItsTestsAndTheCallsTheyAllGoThrough() {
        final Facts.ExternalSystem orders = system("orders-db.internal");
        assertAll(
            () -> assertThat(orders.getTests()).isEqualTo(19),
            () -> assertThat(orders.getSharedChain()).containsExactly(OPEN_QUIETLY, OPEN),
            () -> assertThat(system("main-db.internal").getSharedChain())
                .containsExactly("com.example.alt.DbManager#connect()",
                    "com.example.alt.DefaultProvider#getConnection()"));
    }

    @Test
    void takesWhatASetterGivesOverAStandInHostSuchAsUndefined() {
        assertAll(
            () -> assertThat(system("com.example.addresses.Fetcher").getNamedBy()).isEqualTo("class"),
            () -> assertThat(system("com.example.addresses.Fetcher").getAddress().getValue())
                .isEqualTo("url of target"));
    }
}
