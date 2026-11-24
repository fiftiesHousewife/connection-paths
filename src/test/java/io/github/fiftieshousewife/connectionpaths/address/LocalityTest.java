package io.github.fiftieshousewife.connectionpaths.address;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

class LocalityTest {

    @Test
    void tellsDatabasesInMemoryOrInFilesFromServers() {
        assertAll(
            () -> assertThat(local("jdbc:hsqldb:/opt/app/db")).isTrue(),
            () -> assertThat(local("jdbc:hsqldb:hsql://db.internal/app")).isFalse(),
            () -> assertThat(local("jdbc:h2:mem:test")).isTrue(),
            () -> assertThat(local("jdbc:h2:tcp://db.internal/app")).isFalse(),
            () -> assertThat(local("jdbc:derby:memory:app")).isTrue(),
            () -> assertThat(local("jdbc:derby://db.internal:1527/app")).isFalse(),
            () -> assertThat(local("jdbc:sqlite:app.db")).isTrue());
    }

    @Test
    void treatsThisMachineFilesAndJarsAsLocal() {
        assertAll(
            () -> assertThat(local("http://localhost:8080/")).isTrue(),
            () -> assertThat(local("127.0.0.1")).isTrue(),
            () -> assertThat(local("jar:file:/app.jar!/x")).isTrue(),
            () -> assertThat(local("https://api.internal/")).isFalse());
    }

    @Test
    void treatsAPathAsAFileOnlyForAUrl() {
        final Address path = Address.of(Address.LITERAL, "/META-INF/sidebar.xml");
        assertAll(
            () -> assertThat(Locality.of(path, "url")).contains("a file or a classpath resource"),
            () -> assertThat(Locality.of(path, "http")).isEmpty(),
            () -> assertThat(Locality.of(Address.builder().source(Address.SETTING).value("x").build(), "url"))
                .isEmpty());
    }

    private static boolean local(final String literal) {
        return Locality.of(Address.of(Address.LITERAL, literal), "database").isPresent();
    }
}
