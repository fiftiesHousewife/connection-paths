package io.github.fiftieshousewife.connectionpaths;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

/** Reads the command line into {@link Options}. */
public final class OptionsParser {

    static final String USAGE = """
        under-test-external-calls: finds where Java code opens external connections, and which tests reach them.

        java -jar under-test-external-calls.jar [--module <dir>]... [--main <dir>]... [--test <dir>]...
                                       [--classpath <jars, separated by {separator}> | --classpath @<file>]
                                       [--settings <dir>]... [--rules <file>] [--max-implementations <n>]
                                       [--out <file>]

          --module <dir>            a module: reads <dir>/src/main/java and <dir>/src/test/java
          --main <dir>              a folder of production source
          --test <dir>              a folder of test source
          --classpath               the jars the code compiles against; @<file> reads them from a file, one a line
                                    or separated by {separator}
                                    (Maven: mvn dependency:build-classpath -Dmdep.outputFile=<file>)
          --settings <dir>          a folder whose properties, YAML and XML files hold settings that addresses
                                    name (a --module's own folder is read already)
          --rules <file>            more connection rules, in the form of the built-in connection-rules.txt
          --max-implementations <n> calls through a type with more implementations are reported, not followed
                                    (default {max})
          --out <file>              where to write the facts (default under-test-external-calls.json)
        """.replace("{separator}", File.pathSeparator)
        .replace("{max}", String.valueOf(Options.DEFAULT_MAX_IMPLEMENTATIONS));

    private static final Map<String, BiConsumer<Options.OptionsBuilder, String>> VALUE_FLAGS = Map.of(
        "--classpath", (options, value) -> classpath(value).forEach(options::classpathEntry),
        "--rules", (options, value) -> options.rules(Path.of(value)),
        "--max-implementations", (options, value) -> options.maxImplementations(positive(value)),
        "--out", (options, value) -> options.out(Path.of(value)));
    private static final Set<String> FOLDER_FLAGS = Set.of("--module", "--main", "--test", "--settings");

    private OptionsParser() {
    }

    /** Parses the arguments; throws {@link IllegalArgumentException} with a message for the person if they're wrong. */
    public static Options parse(final List<String> args) {
        if (args.size() % 2 != 0) {
            throw new IllegalArgumentException(args.get(args.size() - 1) + " needs a value");
        }
        final Options.OptionsBuilder options = Options.builder();
        IntStream.range(0, args.size() / 2).forEach(i -> apply(options, args.get(2 * i), args.get(2 * i + 1)));
        return withASourceFolder(options.build());
    }

    private static Options withASourceFolder(final Options parsed) {
        if (parsed.getMainSources().isEmpty() && parsed.getTestSources().isEmpty()) {
            throw new IllegalArgumentException("Give at least one --module, --main or --test folder");
        }
        return parsed;
    }

    private static void apply(final Options.OptionsBuilder options, final String flag, final String value) {
        if (FOLDER_FLAGS.contains(flag)) {
            folder(options, flag, Path.of(value));
            return;
        }
        Optional.ofNullable(VALUE_FLAGS.get(flag))
            .orElseThrow(() -> new IllegalArgumentException("Unknown option " + flag))
            .accept(options, value);
    }

    private static void folder(final Options.OptionsBuilder options, final String flag, final Path folder) {
        switch (flag) {
            case "--module" -> options.mainSource(folder.resolve(Path.of("src", "main", "java")))
                .testSource(folder.resolve(Path.of("src", "test", "java")))
                .settingsFolder(folder);
            case "--main" -> options.mainSource(folder);
            case "--test" -> options.testSource(folder);
            default -> options.settingsFolder(folder);
        }
    }

    static List<Path> classpath(final String value) {
        final boolean fromFile = value.startsWith("@");
        final String text = fromFile ? read(Path.of(value.substring(1))) : value;
        // A file written on Windows may start with a byte order mark.
        final String[] parts = text.replace("\uFEFF", "").split("[\\r\\n" + File.pathSeparator + "]+");
        final List<Path> entries = Arrays.stream(parts)
            .map(String::strip)
            .filter(entry -> !entry.isEmpty())
            .map(Path::of)
            .toList();
        // An empty file is a classpath step that failed or wrote somewhere else: without it, most calls don't resolve.
        if (fromFile && entries.isEmpty()) {
            throw new IllegalArgumentException("The classpath file " + value.substring(1)
                + " names no jar or folder; run the classpath step again, or leave --classpath out");
        }
        return entries;
    }

    private static int positive(final String value) {
        final String flag = "--max-implementations";
        try {
            final int number = Integer.parseInt(value);
            if (number < 1) {
                throw new IllegalArgumentException(flag + " must be 1 or more, not " + value);
            }
            return number;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(flag + " must be a number, not " + value, e);
        }
    }

    private static String read(final Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read the classpath file " + file, e);
        }
    }
}
