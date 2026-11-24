package io.github.fiftieshousewife.connectionpaths.address;

import com.github.javaparser.ast.Node;
import java.util.HashSet;
import java.util.Set;

// One trace of an address: how many steps it has taken, and the values it has already followed, so a value that
// refers back to itself, or a long chain of them, ends.
final class Trace {

    private static final int MAX_STEPS = 40;

    private final Set<Node> followed = new HashSet<>();
    private int steps;

    boolean tooLong() {
        return ++steps > MAX_STEPS;
    }

    boolean firstVisit(final Node value) {
        return followed.add(value);
    }
}
