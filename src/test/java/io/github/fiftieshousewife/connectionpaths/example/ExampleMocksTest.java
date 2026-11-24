package io.github.fiftieshousewife.connectionpaths.example;

import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.ORDERS_TEST;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.onlyReach;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// The example repo's tests that put a mock in place of a store: the chain is kept, and marked where it would run the
// mock instead of the real code.
class ExampleMocksTest {

    private static final String JDBC_ALL = "com.example.orders.JdbcOrderStore#all()";

    @Test
    void marksAChainThroughATypeAMockFieldStandsIn() {
        assertThat(onlyReach("com.example.mocked.StoreWithMockFieldTest.totalsTheOrders").getThroughMock())
            .isEqualTo(JDBC_ALL);
    }

    @Test
    void marksAChainThroughATypeATestHelperMocks() {
        assertThat(onlyReach("com.example.mocked.StoreFromFixturesTest.totalsTheOrders").getThroughMock())
            .isEqualTo(JDBC_ALL);
    }

    @Test
    void leavesAChainThatUsesNoMockUnmarked() {
        assertThat(onlyReach(ORDERS_TEST + ".countsOrders").getThroughMock()).isNull();
    }
}
