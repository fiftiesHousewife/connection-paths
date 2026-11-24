package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.graph.CallGraph;
import io.github.fiftieshousewife.connectionpaths.graph.TestChains;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Groups connection points into the systems tests may reach: by host where the address shows one, else by the
 * setting, variable or JNDI name it comes from, else by the class that opens it. Local connections, host look-ups
 * and processes belong to no system.
 */
public final class SystemsBuilder {

    private static final Set<String> NOT_SYSTEMS = Set.of("host-lookup", "process");
    /** The host in {@code scheme://user@host:port/...}, {@code jdbc:oracle:thin:@host:port} or a bare host name. */
    private static final Pattern HOST = Pattern.compile(
        "(?:[a-z][a-z0-9+.:-]*?(?:://|:@))?(?:[^@/]*@)?([a-z0-9-]+(?:\\.[a-z0-9-]+)+|[a-z0-9-]+)(?::\\d+)?(?:[/;?].*)?",
        Pattern.CASE_INSENSITIVE);

    /** One system while it is being built: its name, how it is named, and its points. */
    private record Group(String name, String namedBy, List<Facts.Point> points) {
    }

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
        final List<Facts.Point> inSystems =
            points.stream().filter(p -> p.getLocal() == null && !NOT_SYSTEMS.contains(p.getKind())).toList();
        final Map<String, Group> byMethod = new TreeMap<>();
        final Map<String, Group> byClass = new TreeMap<>();
        inSystems.stream().map(p -> Map.entry(p.getMethod(), nameOf(p)))
            .filter(e -> !"class".equals(e.getValue().namedBy()))
            .forEach(e -> {
                byMethod.putIfAbsent(e.getKey(), e.getValue());
                byClass.putIfAbsent(typeOf(e.getKey()), e.getValue());
            });
        final Map<String, Group> groups = new TreeMap<>();
        inSystems.forEach(p -> {
            // A point with no address of its own, such as sending on a connection opened in the same method or
            // class, belongs to that connection's system.
            final Group named = nameOf(p);
            final Group group = "class".equals(named.namedBy())
                ? byMethod.getOrDefault(p.getMethod(), byClass.getOrDefault(typeOf(p.getMethod()), named)) : named;
            groups.computeIfAbsent(group.name(), k -> new Group(k, group.namedBy(), new ArrayList<>()))
                .points().add(p);
        });
        final Map<String, Set<String>> implemented = new TreeMap<>();
        groups.forEach((name, group) -> implemented.put(name, implementedBy(group)));
        final Map<MethodKey, List<String>> byTest = new TreeMap<>();
        final List<Facts.ExternalSystem> systems = new ArrayList<>();
        groups.forEach((name, group) -> systems.add(system(group, alternatives(name, implemented), byTest)));
        return new Result(systems, byTest);
    }

    private Facts.ExternalSystem system(final Group group, final List<String> alternatives,
                                        final Map<MethodKey, List<String>> byTest) {
        final Map<MethodKey, MethodKey> next = graph.reachBack(methodsOf(group));
        final List<List<MethodKey>> chains = new ArrayList<>();
        model.getTests().keySet().forEach(test -> TestChains.of(model, test, next).ifPresent(chain -> {
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
            .sharedChain(sharedChain(chains).stream().map(MethodKey::toString).toList())
            .build();
    }

    private Set<MethodKey> methodsOf(final Group group) {
        final Set<String> names = new TreeSet<>(group.points().stream().map(Facts.Point::getMethod).toList());
        final Set<MethodKey> methods = new TreeSet<>();
        model.getPoints().stream().map(SourceModel.ConnectionPoint::method)
            .filter(m -> names.contains(m.toString())).forEach(methods::add);
        return methods;
    }

    /**
     * The longest run of calls at the end of every chain, without the tests themselves: where all the tests' paths to
     * the system have joined.
     */
    static List<MethodKey> sharedChain(final List<List<MethodKey>> chains) {
        if (chains.isEmpty()) {
            return List.of();
        }
        final int shortest = chains.stream().mapToInt(c -> c.size() - 1).min().orElse(0);
        int shared = 0;
        while (shared < shortest && sameFromEnd(chains, shared)) {
            shared++;
        }
        final List<MethodKey> first = chains.get(0);
        return first.subList(first.size() - shared, first.size());
    }

    private static boolean sameFromEnd(final List<List<MethodKey>> chains, final int fromEnd) {
        final List<MethodKey> first = chains.get(0);
        final MethodKey step = first.get(first.size() - 1 - fromEnd);
        return chains.stream().allMatch(c -> c.get(c.size() - 1 - fromEnd).equals(step));
    }

    /** Systems whose points implement the same interface or superclass method as this one's. */
    private static List<String> alternatives(final String name, final Map<String, Set<String>> implemented) {
        final Set<String> mine = implemented.get(name);
        return implemented.entrySet().stream()
            .filter(e -> !e.getKey().equals(name) && e.getValue().stream().anyMatch(mine::contains))
            .map(Map.Entry::getKey).toList();
    }

    /** The supertype methods, declared in the source, that the system's points override. */
    private Set<String> implementedBy(final Group group) {
        final Set<String> declared = new TreeSet<>();
        model.getDeclared().forEach(m -> declared.add(m.overrideKey(m.type())));
        final Set<String> found = new TreeSet<>();
        for (final MethodKey method : methodsOf(group)) {
            model.ancestors(method.type()).stream().map(method::overrideKey).filter(declared::contains)
                .forEach(found::add);
        }
        return found;
    }

    /** A point's system: its host, else what names its address, else the class that opens it. */
    private static Group nameOf(final Facts.Point point) {
        final Address address = point.getAddress();
        final Optional<String> host = hostOf(address);
        if (host.isPresent()) {
            return new Group(host.get(), "host", new ArrayList<>());
        }
        if (address.isNamed()) {
            return new Group(address.getValue(), address.getSource(), new ArrayList<>());
        }
        return new Group(typeOf(point.getMethod()), "class", new ArrayList<>());
    }

    private static String typeOf(final String method) {
        return method.substring(0, method.indexOf('#'));
    }

    static Optional<String> hostOf(final Address address) {
        final String text = Address.SETTING.equals(address.getSource()) ? address.getSettingValue()
            : address.isLiteral() ? address.getValue() : null;
        if (text == null || address.isPlaceholder()) {
            return Optional.empty();
        }
        final Matcher matcher = HOST.matcher(text.strip());
        return matcher.matches() && !matcher.group(1).contains("{") ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
