package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.graph.CallGraph;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// A point whose address is one of its parameters takes what the production code calling it passes, followed back
// caller by caller while that is a parameter too: each distinct one the callers pass. What a test passes is left to
// that test's own address for tests.
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

    // Every distinct address the production callers pass, followed back caller by caller while it is a parameter, the
    // most informative first; or, where no caller passes one, the address as it is.
    List<Address> of(final MethodKey method, final Address address) {
        final List<Address> found = new ArrayList<>();
        final Set<Map.Entry<MethodKey, Address>> seen = new HashSet<>();
        List<Map.Entry<MethodKey, Address>> frontier = List.of(Map.entry(method, address));
        for (int step = 0; !frontier.isEmpty(); step++) {
            final boolean last = step == MOST_STEPS;
            final List<Map.Entry<MethodKey, Address>> next = new ArrayList<>();
            frontier.stream().filter(seen::add).forEach(at -> next.addAll(stepBack(at, last, found)));
            frontier = next;
        }
        return distinctOf(found);
    }

    // What the callers pass for a parameter, or, where none is followed, the address found.
    private List<Map.Entry<MethodKey, Address>> stepBack(final Map.Entry<MethodKey, Address> at, final boolean last,
                                                         final List<Address> found) {
        final List<Map.Entry<MethodKey, Address>> passed = last || ArgumentTracer.parameterOf(at.getValue()).isEmpty()
            ? List.of() : passedInto(at.getKey(), at.getValue());
        if (passed.isEmpty()) {
            found.add(at.getValue());
        }
        return passed;
    }

    // The distinct addresses, the most informative first, leaving out parameters no caller fills unless that is all.
    private static List<Address> distinctOf(final List<Address> found) {
        final List<Address> distinct = found.stream().distinct().sorted(Address.MOST_INFORMATIVE).toList();
        final List<Address> known = distinct.stream().filter(a -> ArgumentTracer.parameterOf(a).isEmpty()).toList();
        return known.isEmpty() ? distinct.subList(0, Math.min(1, distinct.size())) : known;
    }

    private List<Map.Entry<MethodKey, Address>> passedInto(final MethodKey callee, final Address parameter) {
        final String name = ArgumentTracer.parameterOf(parameter).orElseThrow();
        return graph.callersOf(callee).stream().filter(caller -> !model.tests().isTestType(caller.type()))
            .flatMap(caller -> arguments.passedBy(caller, callee, name).map(a -> Map.entry(caller, a)).stream())
            .toList();
    }
}
