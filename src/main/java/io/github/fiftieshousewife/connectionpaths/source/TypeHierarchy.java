package io.github.fiftieshousewife.connectionpaths.source;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** The supertypes recorded for the types in the source, and the class each @Nested test class sits in. */
public final class TypeHierarchy {

    private final Map<String, Set<String>> parents = new HashMap<>();
    private final Map<String, String> enclosing = new HashMap<>();

    void supertype(final String type, final String parent) {
        parents.computeIfAbsent(type, k -> new LinkedHashSet<>()).add(parent);
    }

    // A nested test class runs inside an instance of the class around it, with that class's set-up.
    void nestedIn(final String inner, final String outer) {
        enclosing.put(inner, outer);
    }

    /** Every supertype of a type, following the supertypes recorded for the types in the source. */
    public Set<String> ancestors(final String type) {
        final Set<String> seen = new LinkedHashSet<>();
        final Deque<String> queue = new ArrayDeque<>(parents.getOrDefault(type, Set.of()));
        while (!queue.isEmpty()) {
            final String next = queue.pop();
            if (seen.add(next)) {
                queue.addAll(parents.getOrDefault(next, Set.of()));
            }
        }
        return seen;
    }

    // A test class, its supertypes, and the same for each class a nested test class sits in, nearest first.
    List<String> runningWith(final String testClass) {
        return Stream.iterate(testClass, type -> type != null, enclosing::get)
            .flatMap(type -> Stream.concat(Stream.of(type), ancestors(type).stream()))
            .distinct().toList();
    }
}
