package io.github.fiftieshousewife.connectionpaths.example;

import io.github.fiftieshousewife.connectionpaths.Options;
import io.github.fiftieshousewife.connectionpaths.ConnectionPaths;
import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.assertAddress;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A Spring Data repository reaches the application's data source, not what its call is given. */
class ExampleSpringDataTest {

    private static final String NAMED = "com.example.jdbc.Customers#named(String)";

    @TempDir
    private Path jars;

    @Test
    void givesARepositoryCallTheDataSourceSetting() {
        assertAddress(NAMED, Address.SETTING, Address.DATA_SOURCE_URL);
    }

    @Test
    void takesSpringBootsEmbeddedDatabaseWhenNoSettingNamesOne() throws IOException {
        final Path h2 = jars.resolve("h2-2.3.232.jar");
        try (OutputStream out = Files.newOutputStream(h2);
             JarOutputStream jar = new JarOutputStream(out, new Manifest())) {
            jar.flush();
        }
        final Facts facts = ConnectionPaths.find(Options.builder()
            .mainSource(Path.of("src/test/resources/example/src/main/java"))
            .testSource(Path.of("src/test/resources/example/src/test/java"))
            .settingsFolder(Path.of("src/test/resources/example"))
            .classpathEntry(h2)
            .build());
        assertThat(facts.getConnectionPoints()).filteredOn(p -> p.getMethod().equals(NAMED))
            .extracting(Facts.Point::getLocal).containsExactly("an H2 database in memory or in a file");
    }
}
