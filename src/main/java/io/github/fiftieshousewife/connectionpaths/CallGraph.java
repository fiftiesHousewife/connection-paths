package io.github.fiftieshousewife.connectionpaths;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
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
final class CallGraph {

    private final Map<MethodKey, Set<MethodKey>> callers = new HashMap<>();
    /** Callers by the key overriding matches on (type, name, number of parameters). */
    private final Map<String, Set<MethodKey>> callersByOverrideKey = new HashMap<>();
    /** For each method in the source, the override keys of the supertype methods it overrides and is followed from. */
    private final Map<MethodKey, List<String>> overrides = new HashMap<>();
    private final Map<String, Integer> notFollowed = new TreeMap<>();

    CallGraph(final SourceModel model, final int maxImplementations) {
        model.getCalls().forEach((caller, callees) -> callees.forEach(callee -> addCall(model, caller, callee)));
        overriddenBy(model).forEach((overridden, implementations) -> {
            // Test doubles in the test source don't make a call a dispatch point: only production code counts.
            final long inProduction = implementations.stream().filter(m -> !model.isTestType(m.type())).count();
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
        callersByOverrideKey.computeIfAbsent(callee.overrideKey(callee.type()), k -> new TreeSet<>()).add(caller);
        // A constructor the source doesn't declare still runs the type's field initialisers.
        if (callee.isConstructor() && !model.getDeclared().contains(callee)) {
            callers.computeIfAbsent(MethodKey.fieldsOf(callee.type()), k -> new TreeSet<>()).add(caller);
        }
    }

    /** For each supertype method (by override key), the methods in the source that override it, in a fixed order. */
    private static Map<String, List<MethodKey>> overriddenBy(final SourceModel model) {
        final Map<String, List<MethodKey>> result = new TreeMap<>();
        for (final MethodKey method : new TreeSet<>(model.getDeclared())) {
            if (method.isConstructor() || method.isInitialisers()) {
                continue;
            }
            for (final String ancestor : model.ancestors(method.type())) {
                if (!isJdkOrUnknown(ancestor)) {
                    result.computeIfAbsent(method.overrideKey(ancestor), k -> new ArrayList<>()).add(method);
                }
            }
        }
        return result;
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
    Map<String, Integer> getNotFollowed() {
        return notFollowed;
    }

    /**
     * Walks back from the starting methods to everything that can call them. The result maps each method reached to
     * the next method on its way to a start; a start maps to itself.
     */
    Map<MethodKey, MethodKey> reachBack(final Collection<MethodKey> starts) {
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
            all.addAll(callersByOverrideKey.getOrDefault(overridden, Set.of()));
        }
        return all;
    }

    /** The chain from a method to the start it reaches, following {@link #reachBack}'s result. */
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
