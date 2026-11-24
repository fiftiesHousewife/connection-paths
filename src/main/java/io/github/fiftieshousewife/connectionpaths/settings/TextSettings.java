package io.github.fiftieshousewife.connectionpaths.settings;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Reads settings from properties and YAML files, line by line, so each keeps its line number. */
public final class TextSettings {

    private TextSettings() {
    }

    // Read as java.util.Properties reads them (key=value, key: value or key value; # and ! comments; a line ending in
    // a backslash goes on; escapes), a logical line at a time, so each setting keeps the line it starts on.
    static void properties(final List<String> lines, final SettingsFiles.Found found) {
        int start = 0;
        while (start < lines.size()) {
            final int end = endOfLogicalLine(lines, start);
            final int number = start + 1;
            read(String.join("\n", lines.subList(start, end + 1)))
                .forEach((key, value) -> found.setting(key, value, number));
            start = end + 1;
        }
    }

    // The last physical line of the logical line that starts here: a line that isn't a comment and ends in an odd
    // number of backslashes goes on to the next.
    private static int endOfLogicalLine(final List<String> lines, final int start) {
        int end = start;
        while (end + 1 < lines.size() && !isComment(lines.get(start)) && goesOn(lines.get(end))) {
            end++;
        }
        return end;
    }

    private static boolean isComment(final String line) {
        final String text = line.strip();
        return text.startsWith("#") || text.startsWith("!");
    }

    private static boolean goesOn(final String line) {
        int backslashes = 0;
        for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return backslashes % 2 != 0;
    }

    // A logical line's setting, if it has a value.
    private static Map<String, String> read(final String logical) {
        final Properties one = new Properties();
        try {
            one.load(new StringReader(logical));
        } catch (IOException | IllegalArgumentException e) {
            return Map.of();
        }
        return one.stringPropertyNames().stream().filter(key -> !one.getProperty(key).isBlank())
            .collect(Collectors.toMap(key -> key, one::getProperty));
    }

    // The scalar values of a YAML file's mappings, keyed by their path: spring: datasource: url: x is
    // spring.datasource.url. Lists, anchors and multi-line values are left out.
    static void yaml(final List<String> lines, final SettingsFiles.Found found) {
        final YamlWalk walk = new YamlWalk(found);
        IntStream.range(0, lines.size()).forEach(i -> walk.line(lines.get(i), i + 1));
    }
}
