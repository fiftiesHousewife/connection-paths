package io.github.fiftieshousewife.connectionpaths;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * How a test reaches what {@link CallGraph#reachBack} walked back from: through its own body first, else through what
 * runs with it.
 */
final class TestChains {

    /** The calls from a test to what it reaches, and whether they start in what runs with the test. */
    record TestChain(List<MethodKey> steps, boolean throughSetUp) {
        TestChain {
            steps = List.copyOf(steps);
        }
    }

    private TestChains() {
    }

    static Optional<TestChain> of(final SourceModel model, final MethodKey test,
                                  final Map<MethodKey, MethodKey> next) {
        final MethodKey declared = model.declarationOf(test);
        if (next.containsKey(declared)) {
            return Optional.of(new TestChain(CallGraph.chain(declared, next), false));
        }
        return model.setUpOf(test.type()).stream()
            .filter(next::containsKey)
            .findFirst()
            .map(setUp -> {
                final List<MethodKey> steps = new ArrayList<>(List.of(declared));
                steps.addAll(CallGraph.chain(setUp, next));
                return new TestChain(steps, true);
            });
    }

    /** How a test is named in the facts: its class and method, as a test report names it. */
    static String nameOf(final MethodKey test) {
        return test.type() + "." + test.name();
    }
}
