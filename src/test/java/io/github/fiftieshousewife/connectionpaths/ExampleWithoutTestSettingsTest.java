package io.github.fiftieshousewife.connectionpaths;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

// The example repo read with no settings folder: nothing says which settings its tests run with, so where they would
// connect is unknown, never "none".
class ExampleWithoutTestSettingsTest {

    @Test
    void saysRunTimeNotNoneWhenNoSettingsOnTheTestClasspathAreKnown() {
        final Facts facts = ConnectionPaths.find(Options.builder()
            .mainSource(Path.of("src/test/resources/example/src/main/java"))
            .testSource(Path.of("src/test/resources/example/src/test/java"))
            .build());
        assertThat(facts.getTests())
            .filteredOn(t -> t.getTest().equals("com.example.addresses.AuditFeedTest.readsTheFeed"))
            .flatExtracting(Facts.TestReach::getReaches)
            .filteredOn(r -> r.getKind().equals("url"))
            .extracting(Facts.Reach::getAddressForTests).containsExactly("run-time");
    }
}
