package io.github.fiftieshousewife.connectionpaths.settings;

import java.util.ArrayDeque;
import java.util.Deque;

// Walks a YAML file's lines, keeping the mappings open at each one, and reports each scalar value by its path.
final class YamlWalk {

    // One open mapping: how far it is indented, and its key.
    private record Level(int indent, String key) {
    }

    private final Deque<Level> path = new ArrayDeque<>();
    private final SettingsFiles.Found found;

    YamlWalk(final SettingsFiles.Found found) {
        this.found = found;
    }

    void line(final String raw, final int number) {
        final String line = withoutComment(raw).stripTrailing();
        final String text = line.strip();
        final int colon = text.indexOf(": ");
        final boolean opens = colon < 0 && text.endsWith(":");
        if (!isMapping(text, colon, opens)) {
            return;
        }
        final int indent = line.length() - line.stripLeading().length();
        leaveLevelsAt(indent);
        if (opens) {
            path.push(new Level(indent, text.substring(0, text.length() - 1).strip()));
        } else {
            found.setting(keyOf(text.substring(0, colon).strip()), unquoted(text.substring(colon + 2)), number);
        }
    }

    // Leaves the keys a line at this indent, or less, is no longer under.
    private void leaveLevelsAt(final int indent) {
        while (!path.isEmpty() && path.peek().indent() >= indent) {
            path.pop();
        }
    }

    // A key: value or key: line; not a list item, a document marker or anything else.
    private static boolean isMapping(final String text, final int colon, final boolean opens) {
        return !text.isEmpty() && !text.startsWith("-") && (colon > 0 || opens);
    }

    private String keyOf(final String last) {
        final StringBuilder key = new StringBuilder();
        path.descendingIterator().forEachRemaining(level -> key.append(level.key()).append('.'));
        return key.append(last).toString();
    }

    private static String withoutComment(final String line) {
        final int hash = line.indexOf(" #");
        if (line.strip().startsWith("#")) {
            return "";
        }
        return hash < 0 ? line : line.substring(0, hash);
    }

    private static String unquoted(final String value) {
        final String text = value.strip();
        final boolean quoted = text.length() >= 2 && text.charAt(0) == text.charAt(text.length() - 1)
            && (text.charAt(0) == '"' || text.charAt(0) == '\'');
        return quoted ? text.substring(1, text.length() - 1) : text;
    }
}
