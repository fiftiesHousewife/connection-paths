package io.github.fiftieshousewife.connectionpaths.example;

import io.github.fiftieshousewife.connectionpaths.facts.TestFacts;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.REACHING;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.reachOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
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

    // A JUnit 3 Spring test names its files in getConfigLocations(), where a plain one is at the classpath's root.
    @Test
    void reachesTheBeansOfTheFilesAJunit3SpringTestsConfigLocationsName() {
        assertThat(reachOf("com.example.spring.LedgerSpringTestCase.testLoadsTheLedgerBeans", "database")
            .getPoint()).isEqualTo(BEAN + "ledgerDataSource>()");
    }

    // Two files of the same name, in different folders, are two contexts: each test reaches only the one it names.
    @Test
    void matchesAContextByItsPathOnTheClasspathNotItsFileNameAlone() {
        assertThat(Stream.of("com.example.spring.StubReportsContextTest.loadsTheStubBeans",
                "com.example.spring.ReportsContextTest.loadsTheReportBeans")
            .map(test -> REACHING.get(test).getReaches().stream().map(TestFacts.Reach::getPoint)
                .filter(point -> point.startsWith(BEAN)).sorted().toList()).toList())
            .containsExactly(List.of(BEAN + "stubReportsDataSource>()"),
                List.of(BEAN + "auditDataSource>()", BEAN + "reportsDataSource>()"));
    }

    @Test
    void reachesTheBeansOfEachContextAWildcardLocationMatches() {
        assertThat(reachOf("com.example.spring.EverySpringContextTest.loadsEveryContextInTheFolder", "database")
            .getPoint()).isEqualTo(BEAN + "stubReportsDataSource>()");
    }

    @Test
    void reachesTheBeansOfTheContextCodeLoads() {
        assertThat(reachOf("com.example.spring.LegacyLauncherTest.starts", "database").getChain())
            .contains("com.example.spring.LegacyLauncher#start()").last().asString().startsWith(BEAN);
    }
}
