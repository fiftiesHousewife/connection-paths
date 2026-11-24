package io.github.fiftieshousewife.connectionpaths.graph;

import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.Signatures;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Who calls whom, walked backwards from the methods that open connections. A call through an interface or superclass
 * also reaches the methods in the source that override it, except through the JDK's own types, and except where more
 * than the set number of methods override it: those dispatch points are reported, not followed.
 */
public final class CallGraph {

    private final Map<MethodKey, Set<MethodKey>> callers = new HashMap<>();
    private final Map<String, Map<String, Set<MethodKey>>> callersByOverrideKey = new HashMap<>();
    private final Map<MethodKey, List<String>> overrides = new HashMap<>();
    private final Map<String, Integer> notFollowed = new TreeMap<>();

    public CallGraph(final SourceModel model, final int maxImplementations) {
        model.getCalls().forEach((caller, callees) -> callees.forEach(callee -> addCall(model, caller, callee)));
        overriddenBy(model).forEach((overridden, implementations) -> {
            // Test doubles in the test source don't make a call a dispatch point: only production code counts.
            final long inProduction = implementations.stream().filter(m -> !model.tests().isTestType(m.type())).count();
            if (inProduction > maxImplementations) {
                notFollowed.put(implementations.get(0).inType(typeOf(overridden)).toString(), (int) inProduction);
            } else {
                implementations.forEach(m -> overrides.computeIfAbsent(m, k -> new ArrayList<>()).add(overridden));
            }
        });
    }

    private void addCall(final SourceModel model, final MethodKey caller, final MethodKey callee) {
        callers.computeIfAbsent(callee, k -> new TreeSet<>()).add(caller);
        // Using a type for the first time runs its static initialisers; any use might be the first.
        final MethodKey statics = MethodKey.staticsOf(callee.type());
        if (!statics.equals(callee) && !model.getCalls().getOrDefault(statics, Set.of()).isEmpty()) {
            callers.computeIfAbsent(statics, k -> new TreeSet<>()).add(caller);
        }
        callersByOverrideKey.computeIfAbsent(callee.overrideKey(callee.type()), k -> new HashMap<>())
            .computeIfAbsent(callee.parameters(), k -> new TreeSet<>()).add(caller);
        // A constructor the source doesn't declare still runs the type's field initialisers.
        if (callee.isConstructor() && !model.getDeclared().contains(callee)) {
            callers.computeIfAbsent(MethodKey.fieldsOf(callee.type()), k -> new TreeSet<>()).add(caller);
        }
    }

    // For each supertype method (by override key), the methods in the source that override it, in a fixed order. Only
    // a class whose objects the program creates, or one it creates a subclass of, can have its method called.
    private static Map<String, List<MethodKey>> overriddenBy(final SourceModel model) {
        final Set<String> live = liveTypes(model);
        final Map<String, List<MethodKey>> result = new TreeMap<>();
        new TreeSet<>(model.getDeclared()).stream()
            .filter(method -> !method.isConstructor() && !method.isInitialisers() && live.contains(method.type()))
            .forEach(method -> model.types().ancestors(method.type()).stream()
                .filter(ancestor -> !isJdkOrUnknown(ancestor))
                .forEach(ancestor -> result.computeIfAbsent(method.overrideKey(ancestor), k -> new ArrayList<>())
                    .add(method)));
        return result;
    }

    private static Set<String> liveTypes(final SourceModel model) {
        final Set<String> created = model.created(true);
        final Set<String> live = new HashSet<>(created);
        created.forEach(type -> live.addAll(model.types().ancestors(type)));
        return live;
    }

    private static String typeOf(final String overrideKey) {
        return overrideKey.substring(0, overrideKey.indexOf('#'));
    }

    // Calls through Map, Runnable, Comparable and the like are not followed into the project's own classes: following
    // them joins every Map.put in the code to every class that extends a map.
    private static boolean isJdkOrUnknown(final String type) {
        return type.startsWith("java.") || type.startsWith("javax.") || type.startsWith("?.");
    }

    /** Dispatch points not followed, as a supertype's method, with how many methods override each. */
    public Map<String, Integer> getNotFollowed() {
        return Collections.unmodifiableMap(notFollowed);
    }

    /**
     * Walks back from the starting methods to everything that can call them. The result maps each method reached to
     * the next method on its way to a start; a start maps to itself.
     */
    public Map<MethodKey, MethodKey> reachBack(final Collection<MethodKey> starts) {
        final Map<MethodKey, MethodKey> next = new HashMap<>();
        final Deque<MethodKey> queue = new ArrayDeque<>();
        new TreeSet<>(starts).forEach(start -> {
            next.put(start, start);
            queue.add(start);
        });
        while (!queue.isEmpty()) {
            final MethodKey method = queue.pop();
            for (final MethodKey caller : callersOf(method)) {
                if (!next.containsKey(caller)) {
                    next.put(caller, method);
                    queue.add(caller);
                }
            }
        }
        return next;
    }

    private Set<MethodKey> callersOf(final MethodKey method) {
        final Set<MethodKey> all = new TreeSet<>(callers.getOrDefault(method, Set.of()));
        for (final String overridden : overrides.getOrDefault(method, List.of())) {
            callersByOverrideKey.getOrDefault(overridden, Map.of()).forEach((parameters, callers) -> {
                if (Signatures.couldBeTheSame(parameters, method.parameters())) {
                    all.addAll(callers);
                }
            });
        }
        return all;
    }

    // The chain from a method to the start it reaches, following #reachBack's result.
    static List<MethodKey> chain(final MethodKey from, final Map<MethodKey, MethodKey> next) {
        final List<MethodKey> chain = new ArrayList<>(List.of(from));
        MethodKey current = from;
        while (!next.get(current).equals(current)) {
            current = next.get(current);
            chain.add(current);
        }
        return chain;
    }
}
