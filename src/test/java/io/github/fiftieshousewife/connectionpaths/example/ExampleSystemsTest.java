package io.github.fiftieshousewife.connectionpaths.example;

import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.DB_MANAGER_TEST;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.FACTS;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.OPEN;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.OPEN_QUIETLY;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.REACHING;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.system;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

/** How the example repo's connection points are grouped into the systems tests may reach. */
class ExampleSystemsTest {

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
