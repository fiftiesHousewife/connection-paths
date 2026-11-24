package io.github.fiftieshousewife.connectionpaths.settings;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

// The database Spring Boot starts itself when no setting names one: an embedded database on the classpath, H2, then
// Derby, then HSQLDB, as Spring Boot picks one. The setting's file is the database's jar.
final class EmbeddedDatabases {

    private static final List<Map.Entry<Pattern, String>> IN_ORDER = List.of(
        Map.entry(Pattern.compile("h2-\\d.*\\.jar"), "jdbc:h2:mem:testdb"),
        Map.entry(Pattern.compile("derby-\\d.*\\.jar"), "jdbc:derby:memory:testdb;create=true"),
        Map.entry(Pattern.compile("hsqldb-\\d.*\\.jar"), "jdbc:hsqldb:mem:testdb"));

    private EmbeddedDatabases() {
    }

    static Optional<SettingsFiles.Setting> on(final List<Path> classpath) {
        return IN_ORDER.stream().flatMap(database -> classpath.stream()
                .filter(entry -> Optional.ofNullable(entry.getFileName()).map(Path::toString)
                    .filter(name -> database.getKey().matcher(name).matches()).isPresent())
                .map(jar -> new SettingsFiles.Setting(database.getValue(), jar.toString().replace('\\', '/'), 0)))
            .findFirst();
    }
}
