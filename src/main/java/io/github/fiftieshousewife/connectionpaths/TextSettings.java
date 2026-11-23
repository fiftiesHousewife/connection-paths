package io.github.fiftieshousewife.connectionpaths;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Reads settings from properties and YAML files, line by line, so each keeps its line number. */
final class TextSettings {

    private TextSettings() {
    }

    /** {@code key=value}, {@code key: value} or {@code key value}; comments start with # or !. */
    static void properties(final List<String> lines, final SettingsFiles.Found found) {
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                continue;
            }
            final int split = firstSeparator(line);
            if (split > 0) {
                final String value = line.substring(split + 1).strip();
                found.setting(line.substring(0, split).strip(),
                    value.startsWith("=") || value.startsWith(":") ? value.substring(1).strip() : value, i + 1);
            }
        }
    }

    private static int firstSeparator(final String line) {
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (c == '=' || c == ':' || Character.isWhitespace(c)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The scalar values of a YAML file's mappings, keyed by their path: {@code spring: datasource: url: x} is
     * {@code spring.datasource.url}. Lists, anchors and multi-line values are left out.
     */
    static void yaml(final List<String> lines, final SettingsFiles.Found found) {
        final Deque<Level> path = new ArrayDeque<>();
        for (int i = 0; i < lines.size(); i++) {
            final String line = withoutComment(lines.get(i)).stripTrailing();
            final String text = line.strip();
            final int colon = text.indexOf(": ");
            final boolean opens = colon < 0 && text.endsWith(":");
            if (isMapping(text, colon, opens)) {
                final int indent = line.length() - line.stripLeading().length();
                while (!path.isEmpty() && path.peek().indent() >= indent) {
                    path.pop();
                }
                if (opens) {
                    path.push(new Level(indent, text.substring(0, text.length() - 1).strip()));
                } else {
                    found.setting(keyOf(path, text.substring(0, colon).strip()),
                        unquoted(text.substring(colon + 2)), i + 1);
                }
            }
        }
    }

    /** A {@code key: value} or {@code key:} line; not a list item, a document marker or anything else. */
    private static boolean isMapping(final String text, final int colon, final boolean opens) {
        return !text.isEmpty() && !text.startsWith("-") && (colon > 0 || opens);
    }

    /** One open mapping: how far it is indented, and its key. */
    private record Level(int indent, String key) {
    }

    private static String keyOf(final Deque<Level> path, final String last) {
        final StringBuilder key = new StringBuilder();
        path.descendingIterator().forEachRemaining(level -> key.append(level.key()).append('.'));
        return key.append(last).toString();
    }

    private static String withoutComment(final String line) {
        final int hash = line.indexOf(" #");
        return line.strip().startsWith("#") ? "" : hash < 0 ? line : line.substring(0, hash);
    }

    private static String unquoted(final String value) {
        final String text = value.strip();
        final boolean quoted = text.length() >= 2 && text.charAt(0) == text.charAt(text.length() - 1)
            && (text.charAt(0) == '"' || text.charAt(0) == '\'');
        return quoted ? text.substring(1, text.length() - 1) : text;
    }
}
