package io.github.fiftieshousewife.connectionpaths;

import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.FACTS;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.REACHING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

/** How the example repo's calls are matched to the methods that override them and to the connection rules. */
class ExampleMatchingTest {

    @Test
    void followsACallThroughAnInterfaceOnlyToTheOverloadWithTheSameParameterTypes() {
        assertAll(
            () -> assertThat(REACHING).doesNotContainKey("com.example.overloads.LoaderTest.loadsByName"),
            () -> assertThat(REACHING).containsKey("com.example.overloads.LoaderTest.loadsFromAUrl"));
    }

    @Test
    void matchesAnyMethodRuleThroughASupertypeOnlyForTheMethodsThatSupertypeDeclares() {
        assertAll(
            () -> assertThat(FACTS.getConnectionPoints()).extracting(Facts.Point::getMethod)
                .contains("com.example.jdbc.Customers#count()")
                .doesNotContain("com.example.jdbc.Customers#forget()"),
            () -> assertThat(REACHING).doesNotContainKey("com.example.jdbc.CustomersTest.forgets"));
    }

    @Test
    void matchesAnyMethodRuleOnAnInterfaceThatDeclaresNoneToEveryMethodOfItsSubtypes() {
        assertThat(FACTS.getConnectionPoints()).extracting(Facts.Point::getMethod)
            .contains("com.example.jdbc.Customers#named(String)");
    }
}
