package io.github.fiftieshousewife.connectionpaths.example;

import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.CONFIG_TEST;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.FACTS;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.OPEN;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.OPEN_QUIETLY;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.REACHING;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.STORES_TEST;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.onlyReach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

/**
 * How the example repo's calls are followed through constructors, initialisers, records, nested and TestNG tests,
 * and mocks.
 */
class ExampleJavaFeaturesTest {

    @Test
    void followsAnExplicitSuperclassConstructorCall() {
        assertThat(onlyReach(STORES_TEST + ".namesTheStore").getChain()).containsExactly(
            STORES_TEST + "#namesTheStore()",
            "com.example.stores.NamedStore#<init>()",
            "com.example.stores.BaseStore#<init>(String)",
            OPEN_QUIETLY,
            OPEN);
    }

    @Test
    void runsTheSuperclassFieldInitialisersWhenASubclassIsCreated() {
        assertThat(onlyReach(STORES_TEST + ".sizesTheCache").getChain()).containsExactly(
            STORES_TEST + "#sizesTheCache()",
            "com.example.stores.CachedStore#<fields>()",
            "com.example.stores.BaseCache#<fields>()",
            OPEN_QUIETLY,
            OPEN);
    }

    @Test
    void runsAClassesStaticInitialisersWhenItIsFirstUsed() {
        assertThat(onlyReach(CONFIG_TEST + ".readsASetting").getChain()).containsExactly(
            CONFIG_TEST + "#readsASetting()",
            "com.example.config.Settings#<statics>()",
            OPEN_QUIETLY,
            OPEN);
    }

    @Test
    void readsTheArgumentsOfEnumConstants() {
        assertThat(onlyReach(CONFIG_TEST + ".sizesThePool").getChain()).containsExactly(
            CONFIG_TEST + "#sizesThePool()",
            "com.example.config.Pool#<statics>()",
            OPEN_QUIETLY,
            OPEN);
    }

    @Test
    void followsACallThroughAnInterfaceToARecord() {
        assertThat(onlyReach("com.example.greet.GreetingsTest.says").getChain()).containsExactly(
            "com.example.greet.GreetingsTest#says()",
            "com.example.greet.Greetings#say(Greeter)",
            "com.example.greet.RemoteGreeter#greet(String)",
            OPEN_QUIETLY,
            OPEN);
    }

    @Test
    void appliesTheEnclosingClassesSetUpToANestedTest() {
        final Facts.Reach reach = onlyReach("com.example.ledger.LedgerNestedTest.WhenEmpty.hasNoEntries");
        assertAll(
            () -> assertThat(reach.isThroughSetUp()).isTrue(),
            () -> assertThat(reach.getChain()).containsExactly(
                "com.example.ledger.LedgerNestedTest.WhenEmpty#hasNoEntries()",
                "com.example.ledger.LedgerNestedTest#openTheLedger()",
                "com.example.ledger.Ledger#<fields>()",
                OPEN_QUIETLY,
                OPEN));
    }

    @Test
    void countsTestNgSetUpAndTearDownAsPartOfTheTest() {
        assertAll(
            () -> assertThat(onlyReach("com.example.ledger.LedgerTestNgTest.countsEntries").isThroughSetUp()).isTrue(),
            () -> assertThat(onlyReach("com.example.ledger.LedgerCleanupTest.startsClean").isThroughSetUp()).isTrue());
    }

    @Test
    void doesNotCountACallBeingStubbedOrVerifiedAsReachingIt() {
        assertThat(REACHING).doesNotContainKey("com.example.orders.OrderStoreMockTest.stubsTheStore");
    }

    @Test
    void countsOnlyProductionImplementationsTowardsTheLimit() {
        assertThat(FACTS.getDispatchPointsNotFollowed()).extracting(Facts.Dispatch::method)
            .containsExactly("com.example.events.Listener#changed(String)");
    }

    @Test
    void findsMailThroughATransportOrAStoreAndTheirSubclasses() {
        assertThat(FACTS.getConnectionPoints()).filteredOn(point -> point.getKind().equals("mail"))
            .extracting(Facts.Point::getMethod, Facts.Point::getCall)
            .containsExactlyInAnyOrder(
                tuple("com.example.mail.SmtpMailer#send(String)",
                    "javax.mail.Service#connect"),
                tuple("com.example.mail.SmtpMailer#send(String)",
                    "javax.mail.Transport#sendMessage"),
                tuple("com.example.mail.MailLogin#check(String,String)",
                    "javax.mail.Service#connect"));
    }

    @Test
    void givesTheLineOfTheCallThatConnects() {
        assertThat(FACTS.getConnectionPoints()).filteredOn(point -> point.getMethod().equals(OPEN))
            .extracting(Facts.Point::getLine).containsExactly(9);
    }
}
