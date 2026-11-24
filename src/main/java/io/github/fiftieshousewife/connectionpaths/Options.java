package io.github.fiftieshousewife.connectionpaths;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/** What to read and where to write. Built by {@link OptionsParser} from the command line. */
@Value
@Builder
public class Options {

    static final int DEFAULT_MAX_IMPLEMENTATIONS = 3;

    @Singular
    List<Path> mainSources;
    @Singular
    List<Path> testSources;
    @Singular("classpathEntry")
    List<Path> classpath;
    @Singular("settingsFolder")
    List<Path> settings;
    Path rules;
    @Builder.Default
    int maxImplementations = DEFAULT_MAX_IMPLEMENTATIONS;
    @Builder.Default
    Path out = Path.of("connection-paths.json");

    Optional<Path> extraRules() {
        return Optional.ofNullable(rules);
    }
}
