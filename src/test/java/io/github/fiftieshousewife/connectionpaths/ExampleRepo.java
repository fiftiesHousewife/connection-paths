package io.github.fiftieshousewife.connectionpaths;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// The facts for the example repo in src/test/resources/example, read once, and what its tests look up in them.
final class ExampleRepo {

    static final String ORDERS_TEST = "com.example.orders.OrderServiceTest";
    static final String STORES_TEST = "com.example.stores.StoresTest";
    static final String CONFIG_TEST = "com.example.config.ConfigTest";
    static final String OPEN_QUIETLY = "com.example.db.Database#openQuietly()";
    static final String OPEN = "com.example.db.Database#open()";
    static final String DB_MANAGER_TEST = "com.example.alt.DbManagerTest.connects";
    static final Facts FACTS = ConnectionPaths.find(Options.builder()
        .mainSource(Path.of("src/test/resources/example/src/main/java"))
        .testSource(Path.of("src/test/resources/example/src/test/java"))
        .settingsFolder(Path.of("src/test/resources/example"))
        .build());
    static final Map<String, Facts.TestReach> REACHING =
        FACTS.getTests().stream().collect(Collectors.toMap(Facts.TestReach::getTest, t -> t));

    private ExampleRepo() {
    }

    static Facts.ExternalSystem system(final String name) {
        return FACTS.getSystems().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow();
    }

    static Address addressOf(final String method) {
        return FACTS.getConnectionPoints().stream().filter(point -> point.getMethod().equals(method))
            .findFirst().orElseThrow().getAddress();
    }

    static void assertAddress(final String method, final String source, final String value) {
        final Address address = addressOf(method);
        assertThat(address.getSource() + " " + address.getValue()).isEqualTo(source + " " + value);
    }

    static Facts.Reach reachOf(final String test, final String kind) {
        return REACHING.get(test).getReaches().stream().filter(r -> r.getKind().equals(kind)).findFirst()
            .orElseThrow();
    }

    static Facts.Reach onlyReach(final String test) {
        final List<Facts.Reach> reaches = REACHING.get(test).getReaches();
        assertThat(reaches).hasSize(1);
        return reaches.get(0);
    }

    // The known answers in src/test/resources/expected: one a line, with # for a comment.
    static List<String> expected(final String file) {
        try {
            return Files.readAllLines(Path.of("src/test/resources/expected", file)).stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
