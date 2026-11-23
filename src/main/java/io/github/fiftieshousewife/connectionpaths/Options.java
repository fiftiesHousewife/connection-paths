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

    /** Above this many implementations, a call through an interface or superclass is reported, not followed. */
    static final int DEFAULT_MAX_IMPLEMENTATIONS = 3;

    @Singular
    List<Path> mainSources;
    @Singular
    List<Path> testSources;
    /** The jars (or class folders) the code compiles against: from the build, or listed by a person. */
    @Singular("classpathEntry")
    List<Path> classpath;
    /** Folders whose properties, YAML and XML files are read for the settings that addresses name. */
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
