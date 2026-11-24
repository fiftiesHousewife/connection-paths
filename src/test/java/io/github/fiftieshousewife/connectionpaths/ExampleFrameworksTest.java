package io.github.fiftieshousewife.connectionpaths;

import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.FACTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

/** The example repo's tests that start a framework, which may connect through wiring no call in the source shows. */
class ExampleFrameworksTest {

    @Test
    void listsTestsThatStartAFrameworkByTheAnnotationOnThemOrTheirSuperclass() {
        assertThat(FACTS.getTestsStartingAFramework())
            .extracting(Facts.FrameworkTest::getTest, Facts.FrameworkTest::getFramework,
                Facts.FrameworkTest::getAnnotatedOn)
            .containsExactly(
                tuple("com.example.boot.LedgerContainerTest.storesAnEntry", "@Testcontainers",
                    "com.example.boot.ContainerTestBase"),
                tuple("com.example.boot.LegacyContextTest.loadsTheContext",
                    "@RunWith(SpringJUnit4ClassRunner.class)", "com.example.boot.LegacyContextTest"),
                tuple("com.example.boot.OrdersApplicationTest.startsUp", "@SpringBootTest",
                    "com.example.boot.OrdersApplicationTest"));
    }

    @Test
    void countsThemInTheSummary() {
        assertThat(FACTS.getSummary().getTestsStartingAFramework()).isEqualTo(3);
    }
}
