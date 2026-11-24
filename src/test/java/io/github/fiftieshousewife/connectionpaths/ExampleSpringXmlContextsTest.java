package io.github.fiftieshousewife.connectionpaths;

import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.reachOf;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Tests that load a Spring XML context reach the connections its beans make. */
class ExampleSpringXmlContextsTest {

    private static final String BEAN = "org.apache.commons.dbcp.BasicDataSource#<bean ";

    @Test
    void reachesTheBeansOfTheContextATestClassNames() {
        final Facts.Reach reach = reachOf("com.example.spring.ReportsContextTest.loadsTheReportBeans", "database");
        assertThat(reach.getPoint() + " " + reach.isThroughSetUp()).startsWith(BEAN).endsWith(" true");
    }

    @Test
    void reachesTheBeansOfTheContextCodeLoads() {
        assertThat(reachOf("com.example.spring.LegacyLauncherTest.starts", "database").getChain())
            .contains("com.example.spring.LegacyLauncher#start()").last().asString().startsWith(BEAN);
    }
}
