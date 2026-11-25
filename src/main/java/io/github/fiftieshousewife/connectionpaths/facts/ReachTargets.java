package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.graph.CallGraph;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

// What a test may reach, each walked back from its points: each kind of connection of each system, and each kind's
// points that are in no system, such as those that stay on this machine.
final class ReachTargets {

    private static final String NO_SYSTEM = "";

    // One kind of connection of one system, or of no system, and everything that reaches it.
    record Target(String kind, String system, Map<MethodKey, MethodKey> next) {
    }

    private ReachTargets() {
    }

    static List<Target> of(final SourceModel model, final CallGraph graph,
                           final Map<String, PointGroups.Group> groups) {
        final Map<String, Map<String, Set<MethodKey>>> starts = new TreeMap<>();
        final Set<String> grouped = new TreeSet<>();
        groups.forEach((name, group) -> group.points().forEach(point -> grouped.add(keyOf(point.getKind(),
            point.getMethod()))));
        model.getPoints().forEach(p -> {
            final boolean inASystem = grouped.contains(keyOf(p.kind(), p.method().toString()));
            groups.forEach((name, group) -> group.points().stream()
                .filter(point -> point.getKind().equals(p.kind()) && point.getMethod().equals(p.method().toString()))
                .findFirst().ifPresent(point -> startsOf(starts, p.kind(), name).add(p.method())));
            if (!inASystem) {
                startsOf(starts, p.kind(), NO_SYSTEM).add(p.method());
            }
        });
        final List<Target> targets = new ArrayList<>();
        starts.forEach((kind, bySystem) -> bySystem.forEach((system, methods) -> targets.add(
            new Target(kind, NO_SYSTEM.equals(system) ? null : system, graph.reachBack(methods)))));
        return targets;
    }

    private static Set<MethodKey> startsOf(final Map<String, Map<String, Set<MethodKey>>> starts, final String kind,
                                           final String system) {
        return starts.computeIfAbsent(kind, k -> new TreeMap<>()).computeIfAbsent(system, k -> new TreeSet<>());
    }

    private static String keyOf(final String kind, final String method) {
        return kind + " " + method;
    }
}
