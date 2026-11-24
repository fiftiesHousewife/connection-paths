package io.github.fiftieshousewife.connectionpaths.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsFilesTest {

    @Test
    void readsPropertiesInEachForm(@TempDir final Path dir) throws IOException {
        Files.writeString(dir.resolve("a.properties"), "# comment\n! comment\nurl=x\nhost: y\nport 9\nempty\n");
        final SettingsFiles settings = SettingsFiles.read(List.of(dir));
        assertAll(
            () -> assertThat(settings.get("url"))
                .contains(new SettingsFiles.Setting("x", file(dir, "a.properties"), 3)),
            () -> assertThat(settings.get("host").map(SettingsFiles.Setting::value)).contains("y"),
            () -> assertThat(settings.get("port").map(SettingsFiles.Setting::value)).contains("9"),
            () -> assertThat(settings.get("empty")).isEmpty());
    }

    @Test
    void readsContinuedLinesAndEscapesAsJavaDoes(@TempDir final Path dir) throws IOException {
        Files.writeString(dir.resolve("a.properties"),
            "db.url=jdbc:postgresql://orders.internal:5432/\\\n    orders\nmy\\:key=v\nname=caf\\u00e9\n");
        final SettingsFiles settings = SettingsFiles.read(List.of(dir));
        assertAll(
            () -> assertThat(settings.get("db.url"))
                .contains(new SettingsFiles.Setting("jdbc:postgresql://orders.internal:5432/orders",
                    file(dir, "a.properties"), 1)),
            () -> assertThat(settings.get("my:key").map(SettingsFiles.Setting::line)).contains(3),
            () -> assertThat(settings.get("name").map(SettingsFiles.Setting::value)).contains("caf\u00e9"));
    }

    @Test
    void flattensYamlMappingsAndLeavesListsOut(@TempDir final Path dir) throws IOException {
        Files.writeString(dir.resolve("a.yml"), """
            spring:
              datasource:
                url: "jdbc:h2:mem:test"   # the database
                hosts:
                  - one
              mail: 'smtp.internal'
            server: 8080
            """);
        final SettingsFiles settings = SettingsFiles.read(List.of(dir));
        assertAll(
            () -> assertThat(settings.get("spring.datasource.url").map(SettingsFiles.Setting::value))
                .contains("jdbc:h2:mem:test"),
            () -> assertThat(settings.get("spring.mail").map(SettingsFiles.Setting::value)).contains("smtp.internal"),
            () -> assertThat(settings.get("server").map(SettingsFiles.Setting::line)).contains(7),
            () -> assertThat(settings.size()).isEqualTo(3));
    }

    @Test
    void readsYamlAsAYamlParserDoes(@TempDir final Path dir) throws IOException {
        Files.writeString(dir.resolve("a.yml"), """
            secret: "a #1"
            url: >
              jdbc:postgresql://orders.internal/orders
            db: {host: db.internal, port: 5432}
            """);
        final SettingsFiles settings = SettingsFiles.read(List.of(dir));
        assertAll(
            () -> assertThat(settings.get("secret").map(SettingsFiles.Setting::value)).contains("a #1"),
            () -> assertThat(settings.get("url").map(SettingsFiles.Setting::value))
                .contains("jdbc:postgresql://orders.internal/orders\n"),
            () -> assertThat(settings.get("db.host").map(SettingsFiles.Setting::line)).contains(4));
    }

    @Test
    void readsXmlElementPathsWithAndWithoutTheRootAndNoOutsideEntities(@TempDir final Path dir)
        throws IOException {
        Files.writeString(dir.resolve("a.xml"), """
            <?xml version="1.0"?>
            <!DOCTYPE jive [<!ENTITY outside SYSTEM "file:///etc/passwd">]>
            <jive>
                <database><serverURL>jdbc:x</serverURL></database>
                <secret>&outside;</secret>
            </jive>
            """);
        Files.writeString(dir.resolve("broken.xml"), "<a><b>");
        final SettingsFiles settings = SettingsFiles.read(List.of(dir));
        assertAll(
            () -> assertThat(settings.get("database.serverURL").map(SettingsFiles.Setting::value)).contains("jdbc:x"),
            () -> assertThat(settings.get("jive.database.serverURL")).isPresent(),
            () -> assertThat(settings.get("secret")).isEmpty());
    }

    @Test
    void skipsBuildOutputAndBuildFiles(@TempDir final Path dir) throws IOException {
        Files.createDirectories(dir.resolve("target"));
        Files.writeString(dir.resolve("target/a.properties"), "a=1\n");
        Files.writeString(dir.resolve("pom.xml"), "<project><name>x</name></project>");
        assertThat(SettingsFiles.read(List.of(dir, dir.resolve("missing"))).size()).isZero();
    }

    @Test
    void recognisesPlaceholders() {
        assertAll(
            () -> assertThat(SettingsFiles.isPlaceholder("https://YOUR-CROWD-SERVER:8095/crowd/")).isTrue(),
            () -> assertThat(SettingsFiles.isPlaceholder("jdbc:<host>/db")).isTrue(),
            () -> assertThat(SettingsFiles.isPlaceholder("${DB_URL}")).isTrue(),
            () -> assertThat(SettingsFiles.isPlaceholder("CHANGE-ME")).isTrue(),
            () -> assertThat(SettingsFiles.isPlaceholder("smtp.Example.com")).isTrue(),
            () -> assertThat(SettingsFiles.isPlaceholder("http://undefined/")).isTrue(),
            () -> assertThat(SettingsFiles.isPlaceholder("Your order")).isFalse(),
            () -> assertThat(SettingsFiles.isPlaceholder("db.internal")).isFalse(),
            () -> assertThat(SettingsFiles.isPlaceholder(null)).isFalse());
    }

    private static String file(final Path dir, final String name) {
        return dir.resolve(name).toString().replace('\\', '/');
    }

    @Test
    void takesAResourcesFolderGivenAsARelativePathAsOnTheTestClasspath() {
        assertThat(SettingsFileFinder.isOnTheTestClasspath(Path.of("src/main/resources/application.properties")))
            .isTrue();
    }
}
