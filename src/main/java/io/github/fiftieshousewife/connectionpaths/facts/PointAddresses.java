package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.graph.CallGraph;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import java.util.Map;
import java.util.Optional;

// A point whose address is one of its parameters takes what the production code calling it passes, followed back
// caller by caller while that is a parameter too: the most informative of what the callers pass. What a test passes
// is left to that test's own address for tests.
final class PointAddresses {

    private static final int MOST_STEPS = 8;

    private final SourceModel model;
    private final CallGraph graph;
    private final ArgumentTracer arguments;

    PointAddresses(final SourceModel model, final CallGraph graph) {
        this.model = model;
        this.graph = graph;
        this.arguments = new ArgumentTracer(model);
    }

    Address of(final MethodKey method, final Address address) {
        Map.Entry<MethodKey, Address> at = Map.entry(method, address);
        for (int step = 0; step < MOST_STEPS && ArgumentTracer.parameterOf(at.getValue()).isPresent(); step++) {
            final Optional<Map.Entry<MethodKey, Address>> passed = passedInto(at.getKey(), at.getValue());
            if (passed.isEmpty()) {
                break;
            }
            at = passed.get();
        }
        return at.getValue();
    }

    private Optional<Map.Entry<MethodKey, Address>> passedInto(final MethodKey callee, final Address parameter) {
        final String name = ArgumentTracer.parameterOf(parameter).orElseThrow();
        return graph.callersOf(callee).stream().filter(caller -> !model.tests().isTestType(caller.type()))
            .flatMap(caller -> arguments.passedBy(caller, callee, name).map(a -> Map.entry(caller, a)).stream())
            .min(Map.Entry.comparingByValue(Address.MOST_INFORMATIVE));
    }
}
