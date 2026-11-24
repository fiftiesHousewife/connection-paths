package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import java.util.List;
import java.util.stream.IntStream;

// The longest run of calls at the end of every chain, without the tests themselves: where all the tests' paths to a
// system have joined.
final class SharedChains {

    private SharedChains() {
    }

    static List<MethodKey> of(final List<List<MethodKey>> chains) {
        if (chains.isEmpty()) {
            return List.of();
        }
        final int shortest = chains.stream().mapToInt(c -> c.size() - 1).min().orElse(0);
        final int shared = IntStream.range(0, shortest).filter(fromEnd -> !sameFromEnd(chains, fromEnd)).findFirst()
            .orElse(shortest);
        final List<MethodKey> first = chains.get(0);
        return first.subList(first.size() - shared, first.size());
    }

    private static boolean sameFromEnd(final List<List<MethodKey>> chains, final int fromEnd) {
        final List<MethodKey> first = chains.get(0);
        final MethodKey step = first.get(first.size() - 1 - fromEnd);
        return chains.stream().allMatch(c -> c.get(c.size() - 1 - fromEnd).equals(step));
    }
}
