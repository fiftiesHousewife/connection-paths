package io.github.fiftieshousewife.connectionpaths.graph;

import io.github.fiftieshousewife.connectionpaths.source.Mark;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A call that matches a rule but goes into a client whose code is in the source, such as a library's own tests
 * calling its client, is no connection point: the client's code opens the connection itself, and a chain follows it
 * there. A client in the source whose code opens none, such as one written as a stand-in, stays a point.
 */
public final class ClientsInTheSource {

    private ClientsInTheSource() {
    }

    /** Marks each point whose rule's call goes into the source's own code, where that reaches another point. */
    public static void mark(final SourceModel model, final CallGraph graph) {
        final Set<SourceModel.ConnectionPoint> points = model.getPoints();
        final Map<MethodKey, MethodKey> reaching = graph.reachBack(points.stream()
            .map(SourceModel.ConnectionPoint::method).collect(Collectors.toSet()));
        points.stream().filter(point -> callees(model, point).anyMatch(reaching::containsKey))
            .forEach(point -> model.mark(Mark.NOT_A_POINT, point.toString()));
    }

    // The methods in the source the point's call may be to: of the rule's name, in its type or a subtype.
    private static Stream<MethodKey> callees(final SourceModel model,
                                                              final SourceModel.ConnectionPoint point) {
        final int hash = point.call().indexOf('#');
        if (point.call().startsWith("@") || hash < 0) {
            return Stream.empty();
        }
        final String type = point.call().substring(0, hash);
        final String name = point.call().substring(hash + 1);
        return model.getCalls().getOrDefault(point.method(), Set.of()).stream()
            .filter(callee -> callee.name().equals(name) && model.getDeclared().contains(callee))
            .filter(callee -> callee.type().equals(type) || model.types().ancestors(callee.type()).contains(type))
            .filter(callee -> !callee.equals(point.method()));
    }
}
