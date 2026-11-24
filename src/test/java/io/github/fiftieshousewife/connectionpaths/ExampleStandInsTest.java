package io.github.fiftieshousewife.connectionpaths;

import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.REACHING;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Servers a test class starts on this machine for its tests, in its set-up or as a container. */
class ExampleStandInsTest {

    @Test
    void namesAServerTheSetUpStartsThroughAHelper() {
        assertThat(REACHING.get("com.example.report.ReportClientWithServerTest.fetches").getStandIns())
            .containsExactly("org.eclipse.jetty.server.Server");
    }

    @Test
    void namesAContainerTheTestClassStarts() {
        assertThat(REACHING.get("com.example.boot.LedgerContainerTest.storesAnEntry").getStandIns())
            .containsExactly("org.testcontainers.containers.PostgreSQLContainer");
    }
}
