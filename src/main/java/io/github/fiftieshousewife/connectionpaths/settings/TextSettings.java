package io.github.fiftieshousewife.connectionpaths.settings;

import java.util.List;
import java.util.stream.IntStream;

/** Reads settings from properties and YAML files, line by line, so each keeps its line number. */
public final class TextSettings {

    private TextSettings() {
    }

    // key=value, key: value or key value; comments start with # or !.
    static void properties(final List<String> lines, final SettingsFiles.Found found) {
        IntStream.range(0, lines.size()).forEach(i -> property(lines.get(i).strip(), i + 1, found));
    }

    private static void property(final String line, final int number, final SettingsFiles.Found found) {
        final int split = isComment(line) ? -1 : firstSeparator(line);
        if (split > 0) {
            found.setting(line.substring(0, split).strip(), valueAfter(line, split), number);
        }
    }

    private static boolean isComment(final String line) {
        return line.isEmpty() || line.startsWith("#") || line.startsWith("!");
    }

    private static int firstSeparator(final String line) {
        return IntStream.range(0, line.length()).filter(i -> isSeparator(line.charAt(i))).findFirst().orElse(-1);
    }

    private static boolean isSeparator(final char c) {
        return c == '=' || c == ':' || Character.isWhitespace(c);
    }

    // The value after the first separator, and after an = or : that follows spaces, as in "key = value".
    private static String valueAfter(final String line, final int split) {
        final String value = line.substring(split + 1).strip();
        return value.startsWith("=") || value.startsWith(":") ? value.substring(1).strip() : value;
    }

    // The scalar values of a YAML file's mappings, keyed by their path: spring: datasource: url: x is
    // spring.datasource.url. Lists, anchors and multi-line values are left out.
    static void yaml(final List<String> lines, final SettingsFiles.Found found) {
        final YamlWalk walk = new YamlWalk(found);
        IntStream.range(0, lines.size()).forEach(i -> walk.line(lines.get(i), i + 1));
    }
}
