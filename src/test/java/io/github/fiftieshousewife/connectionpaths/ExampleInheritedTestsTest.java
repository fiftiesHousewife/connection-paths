package io.github.fiftieshousewife.connectionpaths;

import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.OPEN;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.OPEN_QUIETLY;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.REACHING;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.onlyReach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

/** The example repo's test declared in an abstract class, which each subclass runs with its own set-up. */
class ExampleInheritedTestsTest {

    private static final String PACKAGE = "com.example.inherited.";

    @Test
    void listsAnInheritedTestUnderTheSubclassThatRunsItWithThatSubclasssSetUp() {
        final Facts.Reach reach = onlyReach(PACKAGE + "LedgerResourceTest.isReady");
        assertAll(
            () -> assertThat(reach.isThroughSetUp()).isTrue(),
            () -> assertThat(reach.getChain()).containsExactly(
                PACKAGE + "ResourceContractTest#isReady()",
                PACKAGE + "LedgerResourceTest#createResource()",
                "com.example.ledger.Ledger#<fields>()",
                OPEN_QUIETLY,
                OPEN),
            () -> assertThat(REACHING).doesNotContainKey(PACKAGE + "TextResourceTest.isReady"));
    }
}
