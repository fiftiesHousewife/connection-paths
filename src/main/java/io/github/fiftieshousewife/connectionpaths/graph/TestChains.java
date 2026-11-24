package io.github.fiftieshousewife.connectionpaths.graph;

import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * How a test reaches what {@link CallGraph#reachBack} walked back from: through its own body first, else through what
 * runs with it.
 */
public final class TestChains {

    /** The calls from a test to what it reaches, and whether they start in what runs with the test. */
    public record TestChain(List<MethodKey> steps, boolean throughSetUp) {
        public TestChain {
            steps = List.copyOf(steps);
        }
    }

    private TestChains() {
    }

    public static Optional<TestChain> of(final SourceModel model, final MethodKey test,
                                  final Map<MethodKey, MethodKey> next) {
        final MethodKey declared = model.tests().declarationOf(test);
        if (next.containsKey(declared)) {
            return Optional.of(new TestChain(CallGraph.chain(declared, next), false));
        }
        return throughSetUp(model, test, next);
    }

    /** How a test reaches what {@link CallGraph#reachBack} walked back from through what runs with it, if it does. */
    public static Optional<TestChain> throughSetUp(final SourceModel model, final MethodKey test,
                                                   final Map<MethodKey, MethodKey> next) {
        final MethodKey declared = model.tests().declarationOf(test);
        return model.tests().setUpOf(test.type()).stream()
            .filter(next::containsKey)
            .findFirst()
            .map(setUp -> {
                final List<MethodKey> steps = new ArrayList<>(List.of(declared));
                steps.addAll(CallGraph.chain(setUp, next));
                return new TestChain(steps, true);
            });
    }

    /** How a test is named in the facts: its class and method, as a test report names it. */
    public static String nameOf(final MethodKey test) {
        return test.type() + "." + test.name();
    }
}
