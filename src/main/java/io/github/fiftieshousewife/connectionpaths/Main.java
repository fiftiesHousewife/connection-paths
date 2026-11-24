package io.github.fiftieshousewife.connectionpaths;

import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/** The command line: reads the source folders, writes the facts file, and prints a short summary. */
@Slf4j
public final class Main {

    static final int OK = 0;
    static final int FAILED = 1;
    static final int WRONG_ARGUMENTS = 2;

    private Main() {
    }

    public static void main(final String[] args) {
        final int status = run(List.of(args));
        if (status != OK) {
            System.exit(status);
        }
    }

    static int run(final List<String> args) {
        if (args.isEmpty() || args.contains("--help")) {
            log.info(OptionsParser.USAGE);
            return args.isEmpty() ? WRONG_ARGUMENTS : OK;
        }
        return parsed(args).filter(Main::hasASourceFolder).map(Main::findAndWrite).orElse(WRONG_ARGUMENTS);
    }

    private static Optional<Options> parsed(final List<String> args) {
        try {
            return Optional.of(OptionsParser.parse(args));
        } catch (IllegalArgumentException | UncheckedIOException e) {
            log.error("{}\n\n{}", e.getMessage(), OptionsParser.USAGE);
            return Optional.empty();
        }
    }

    private static boolean hasASourceFolder(final Options options) {
        final boolean any = Stream.concat(options.getMainSources().stream(), options.getTestSources().stream())
            .anyMatch(Files::isDirectory);
        if (!any) {
            log.error("None of the source folders exists: {} {}", options.getMainSources(), options.getTestSources());
        }
        return any;
    }

    private static int findAndWrite(final Options options) {
        try {
            final Facts facts = ConnectionPaths.find(options);
            ConnectionPaths.write(facts, options.getOut());
            summarise(facts, options);
            return OK;
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            return WRONG_ARGUMENTS;
        } catch (UncheckedIOException e) {
            log.error(e.getMessage());
            return FAILED;
        }
    }

    private static void summarise(final Facts facts, final Options options) {
        final Facts.Summary s = facts.getSummary();
        final int calls = s.getCallsResolved() + s.getCallsNotResolved();
        log.info("Read {} files ({} not parsed), {} methods; {} of {} calls resolved ({}%).", s.getFiles(),
            s.getFilesNotParsed(), s.getMethods(), s.getCallsResolved(), calls, percent(s.getCallsResolved(), calls));
        log.info("{} connection points; {} of {} tests may reach one; {} dispatch points not followed.",
            facts.getConnectionPoints().size(), s.getTestsReaching(), s.getTests(),
            facts.getDispatchPointsNotFollowed().size());
        facts.getClasspathNotes().forEach(note -> log.info("Classpath: {}", note));
        log.info("Facts written to {}", options.getOut());
    }

    private static String percent(final int part, final int whole) {
        return whole == 0 ? "100" : String.format(java.util.Locale.ROOT, "%.1f", 100.0 * part / whole);
    }
}
