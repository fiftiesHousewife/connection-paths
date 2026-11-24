package io.github.fiftieshousewife.connectionpaths.example;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.FACTS;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.system;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

/** The example repo's beans defined in Spring XML, which open connections the Java source never shows. */
class ExampleSpringXmlTest {

    private static final String REPORTS = "org.apache.commons.dbcp.BasicDataSource#<bean reportsDataSource>()";
    private static final String AUDIT = "org.apache.commons.dbcp.BasicDataSource#<bean auditDataSource>()";

    @Test
    void findsABeanWhoseClassIsAConnectionClientAtItsFileAndLine() {
        final Facts.Point reports = point(REPORTS);
        assertAll(
            () -> assertThat(reports.getKind()).isEqualTo("database"),
            () -> assertThat(reports.getCall()).isEqualTo("javax.sql.DataSource#getConnection"),
            () -> assertThat(reports.getFile()).endsWith("src/main/resources/applicationContext.xml"),
            () -> assertThat(reports.getLine()).isEqualTo(5),
            () -> assertThat(FACTS.getConnectionPoints()).extracting(Facts.Point::getMethod)
                .noneMatch(method -> method.contains("<bean reportJob>")));
    }

    @Test
    void takesTheAddressFromTheBeansUrlPropertyAndLooksItsSettingUp() {
        final Address address = point(REPORTS).getAddress();
        assertAll(
            () -> assertThat(address.getValue()).isEqualTo("reports.db.url"),
            () -> assertThat(address.getVia()).isEqualTo("Spring XML"),
            () -> assertThat(system("reports-db.internal").getPoints()).containsExactly(REPORTS),
            () -> assertThat(point(AUDIT).getLocal()).isEqualTo("an H2 database in memory or in a file"));
    }

    private static Facts.Point point(final String method) {
        return FACTS.getConnectionPoints().stream().filter(p -> p.getMethod().equals(method)).findFirst().orElseThrow();
    }
}
