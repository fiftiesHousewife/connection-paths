package io.github.fiftieshousewife.connectionpaths.source;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The calls the symbol solver couldn't resolve, counted by reason, with the first place each reason was seen. */
public final class Unresolved {

    private static final int MESSAGE_LENGTH = 90;

    /** A reason, how many calls it stopped, and one example. */
    public record Reason(String reason, int count, String example) {
    }

    private final Map<String, Integer> counts = new HashMap<>();
    private final Map<String, String> examples = new HashMap<>();
    private int total;

    /** Records a failure; similar messages share a reason, with names of types and symbols taken out. */
    void add(final Throwable failure, final SourceModel.Location at) {
        final String message = String.valueOf(failure.getMessage()).replaceAll("\\s+", " ").strip();
        final String reason = failure.getClass().getSimpleName() + ": " + generalise(message);
        counts.merge(reason, 1, Integer::sum);
        examples.putIfAbsent(reason, at.file() + ":" + at.line() + " " + shorten(message));
        total++;
    }

    public int getTotal() {
        return total;
    }

    /** The reasons, most frequent first, at most {@code limit} of them. */
    public List<Reason> top(final int limit) {
        return counts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()))
            .limit(limit)
            .map(e -> new Reason(e.getKey(), e.getValue(), examples.get(e.getKey())))
            .toList();
    }

    /**
     * A missing type keeps its name, also where it is a call's scope, so the report shows which library is missing;
     * messages about whole expressions and quoted names are made general, so they group.
     */
    static String generalise(final String message) {
        final String general = message
            .replaceAll("^Unsolved symbol in .* : ((?:[a-z0-9_$]+\\.)*[A-Z][A-Za-z0-9_$]*)$", "Unsolved symbol : $1")
            .replaceAll("^Unsolved symbol in .*", "Unsolved symbol in an expression")
            .replaceAll("'[^']*'", "'_'");
        return shorten(general);
    }

    private static String shorten(final String text) {
        return text.length() > MESSAGE_LENGTH ? text.substring(0, MESSAGE_LENGTH) + "..." : text;
    }
}
