package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.graph.CallGraph;
import io.github.fiftieshousewife.connectionpaths.graph.TestChains;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Groups connection points into the systems tests may reach: by host where the address shows one, else by the
 * setting, variable or JNDI name it comes from, else by the class that opens it. Local connections, host look-ups
 * and processes belong to no system.
 */
public final class SystemsBuilder {

    /** What the systems are, and which each test reaches. */
    record Result(List<Facts.ExternalSystem> systems, Map<MethodKey, List<String>> byTest) {
    }

    private final SourceModel model;
    private final CallGraph graph;

    SystemsBuilder(final SourceModel model, final CallGraph graph) {
        this.model = model;
        this.graph = graph;
    }

    Result build(final List<Facts.Point> points) {
        final Map<String, PointGroups.Group> groups = PointGroups.of(points);
        final Map<String, Set<String>> implemented = new TreeMap<>();
        groups.forEach((name, group) -> implemented.put(name, implementedBy(group)));
        final Map<MethodKey, List<String>> byTest = new TreeMap<>();
        final List<Facts.ExternalSystem> systems = new ArrayList<>();
        groups.forEach((name, group) -> systems.add(system(group, alternatives(name, implemented), byTest)));
        return new Result(systems, byTest);
    }

    private Facts.ExternalSystem system(final PointGroups.Group group, final List<String> alternatives,
                                        final Map<MethodKey, List<String>> byTest) {
        final Map<MethodKey, MethodKey> next = graph.reachBack(methodsOf(group));
        final List<List<MethodKey>> chains = new ArrayList<>();
        model.tests().all().keySet().forEach(test -> TestChains.of(model, test, next).ifPresent(chain -> {
            chains.add(chain.steps());
            byTest.computeIfAbsent(test, k -> new ArrayList<>()).add(group.name());
        }));
        return Facts.ExternalSystem.builder()
            .name(group.name())
            .namedBy(group.namedBy())
            .kinds(group.points().stream().map(Facts.Point::getKind).distinct().sorted().toList())
            .address(group.points().get(0).getAddress())
            .points(group.points().stream().map(Facts.Point::getMethod).distinct().toList())
            .alternatives(alternatives)
            .tests(chains.size())
            .sharedChain(SharedChains.of(chains).stream().map(MethodKey::toString).toList())
            .build();
    }

    private Set<MethodKey> methodsOf(final PointGroups.Group group) {
        final Set<String> names = new TreeSet<>(group.points().stream().map(Facts.Point::getMethod).toList());
        final Set<MethodKey> methods = new TreeSet<>();
        model.getPoints().stream().map(SourceModel.ConnectionPoint::method)
            .filter(m -> names.contains(m.toString())).forEach(methods::add);
        return methods;
    }

    // Systems whose points implement the same interface or superclass method as this one's.
    private static List<String> alternatives(final String name, final Map<String, Set<String>> implemented) {
        final Set<String> mine = implemented.get(name);
        return implemented.entrySet().stream()
            .filter(e -> !e.getKey().equals(name) && e.getValue().stream().anyMatch(mine::contains))
            .map(Map.Entry::getKey).toList();
    }

    // The supertype methods, declared in the source, that the system's points override.
    private Set<String> implementedBy(final PointGroups.Group group) {
        final Set<String> declared = new TreeSet<>();
        model.getDeclared().forEach(m -> declared.add(m.overrideKey(m.type())));
        final Set<String> found = new TreeSet<>();
        methodsOf(group).forEach(method -> model.types().ancestors(method.type()).stream()
            .map(method::overrideKey).filter(declared::contains).forEach(found::add));
        return found;
    }
}
