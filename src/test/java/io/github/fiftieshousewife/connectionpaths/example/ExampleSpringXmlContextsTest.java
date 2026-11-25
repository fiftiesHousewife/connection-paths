package io.github.fiftieshousewife.connectionpaths.example;

import io.github.fiftieshousewife.connectionpaths.facts.TestFacts;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.reachOf;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Tests that load a Spring XML context reach the connections its beans make. */
class ExampleSpringXmlContextsTest {

    private static final String BEAN = "org.apache.commons.dbcp.BasicDataSource#<bean ";

    @Test
    void reachesTheBeansOfTheContextATestClassNames() {
        final TestFacts.Reach reach = reachOf("com.example.spring.ReportsContextTest.loadsTheReportBeans", "database");
        assertThat(reach.getPoint() + " " + reach.isThroughSetUp()).startsWith(BEAN).endsWith(" true");
    }

    @Test
    void reachesTheBeansOfTheContextASuperclassNames() {
        assertThat(reachOf("com.example.spring.ReportsContextSubclassTest.loadsTheBeansItsSuperclassNames", "database")
            .getPoint()).startsWith(BEAN);
    }

    @Test
    void reachesTheBeansOfAFileTheContextImports() {
        assertThat(reachOf("com.example.spring.LedgerContextTest.loadsTheBeansItsContextImports", "database")
            .getPoint()).isEqualTo(BEAN + "ledgerDataSource>()");
    }

    @Test
    void reachesTheBeansOfTheContextCodeLoads() {
        assertThat(reachOf("com.example.spring.LegacyLauncherTest.starts", "database").getChain())
            .contains("com.example.spring.LegacyLauncher#start()").last().asString().startsWith(BEAN);
    }
}
