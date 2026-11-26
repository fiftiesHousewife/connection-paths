package io.github.fiftieshousewife.connectionpaths.example;

import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.FACTS;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.reachOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

/** The example repo's tests that start a framework, which may connect through wiring no call in the source shows. */
class ExampleFrameworksTest {

    @Test
    void listsTestsThatStartAFrameworkByTheAnnotationOnThemOrTheirSuperclass() {
        assertThat(FACTS.getTestsStartingAFramework())
            .filteredOn(test -> test.getTest().startsWith("com.example.boot."))
            .extracting(Facts.FrameworkTest::getTest, Facts.FrameworkTest::getFramework,
                Facts.FrameworkTest::getAnnotatedOn)
            .containsExactly(
                tuple("com.example.boot.LedgerContainerTest.storesAnEntry", "@Testcontainers",
                    "com.example.boot.ContainerTestBase"),
                tuple("com.example.boot.LegacyContextTest.loadsTheContext",
                    "@RunWith(SpringJUnit4ClassRunner.class)", "com.example.boot.LegacyContextTest"),
                tuple("com.example.boot.OrdersApplicationTest.startsUp", "@SpringBootTest",
                    "com.example.boot.OrdersApplicationTest"),
                tuple("com.example.boot.OrdersOverHttpTest.showsAnOrder",
                    "@SpringBootTest(webEnvironment = RANDOM_PORT)", "com.example.boot.OrdersOverHttpTest"));
    }

    @Test
    void listsTestsThatNameASpringContextByTheAnnotationOnThemOrTheirSuperclass() {
        assertThat(FACTS.getTestsStartingAFramework())
            .filteredOn(test -> test.getTest().startsWith("com.example.spring."))
            .extracting(Facts.FrameworkTest::getTest, Facts.FrameworkTest::getFramework,
                Facts.FrameworkTest::getAnnotatedOn)
            .containsExactly(
                tuple("com.example.spring.EverySpringContextTest.loadsEveryContextInTheFolder", "@ContextConfiguration",
                    "com.example.spring.EverySpringContextTest"),
                tuple("com.example.spring.LedgerContextTest.loadsTheBeansItsContextImports", "@ContextConfiguration",
                    "com.example.spring.LedgerContextTest"),
                tuple("com.example.spring.ReportsContextSubclassTest.loadsTheBeansItsSuperclassNames",
                    "@ContextConfiguration", "com.example.spring.ReportsContextBase"),
                tuple("com.example.spring.ReportsContextTest.loadsTheReportBeans", "@ContextConfiguration",
                    "com.example.spring.ReportsContextTest"),
                tuple("com.example.spring.StubReportsContextTest.loadsTheStubBeans", "@ContextConfiguration",
                    "com.example.spring.StubReportsContextTest"));
    }

    @Test
    void countsThemInTheSummary() {
        assertThat(FACTS.getSummary().getTestsStartingAFramework()).isEqualTo(10);
    }

    // The framework makes what a test class's @Autowired field holds, though no code the test runs does.
    @Test
    void reachesThroughWhatTheFrameworkPutsInATestsInjectedField() {
        assertThat(reachOf("com.example.addresses.DevPriceFeedsTest.readsTheDevRates", "url").getChain())
            .contains("com.example.addresses.PriceFeeds#rates()");
    }
}
