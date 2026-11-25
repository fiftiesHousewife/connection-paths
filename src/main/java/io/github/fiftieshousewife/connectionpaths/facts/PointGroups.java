package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Groups connection points by the system they go to: by host where the address shows one, else by the setting,
// variable or JNDI name it comes from, else by the class that opens it. A point with no address of its own, such as
// sending on a connection opened in the same method or class, belongs to that connection's system; but a call its
// callers give hosts, and one known only at run time, is in its class's system for the one known at run time. Local
// connections, host look-ups and processes belong to no system. A host's name may be internationalised
// (räksmörgås.josefsson.org), so its parts are letters and digits of any script.
final class PointGroups {

    private static final Set<String> NOT_SYSTEMS = Set.of("host-lookup", "process");
    private static final String CLASS = "class";
    private static final String PART = "[\\p{L}\\p{N}-]+";
    private static final Pattern HOST = Pattern.compile("(?:[a-z][a-z0-9+.:-]*?(?:://|:@))?(?:[^@/]*@)?(" + PART
        + "(?:\\." + PART + ")+|" + PART + ")(?::\\d+)?(?:[/;?].*)?", Pattern.CASE_INSENSITIVE);

    // One system while it is being built: its name, how it is named, and its points.
    record Group(String name, String namedBy, List<Facts.Point> points) {
    }

    private PointGroups() {
    }

    static Map<String, Group> of(final List<Facts.Point> points) {
        final List<Facts.Point> inSystems =
            points.stream().filter(p -> p.getLocal() == null && !NOT_SYSTEMS.contains(p.getKind())).toList();
        final Map<String, Group> byMethod = firstNamed(inSystems, Facts.Point::getMethod);
        final Map<String, Group> byClass = firstNamed(inSystems, p -> typeOf(p.getMethod()));
        final Set<String> namedCalls = inSystems.stream().filter(p -> !CLASS.equals(nameOf(p).namedBy()))
            .map(PointGroups::callOf).collect(Collectors.toSet());
        final Map<String, Group> groups = new TreeMap<>();
        inSystems.forEach(p -> {
            final Group named = nameOf(p);
            final Group group = !CLASS.equals(named.namedBy()) || namedCalls.contains(callOf(p)) ? named
                : byMethod.getOrDefault(p.getMethod(), byClass.getOrDefault(typeOf(p.getMethod()), named));
            groups.computeIfAbsent(group.name(), k -> new Group(k, group.namedBy(), new ArrayList<>())).points().add(p);
        });
        return groups;
    }

    // The first system named by an address, for each method or class.
    private static Map<String, Group> firstNamed(final List<Facts.Point> points,
                                                 final Function<Facts.Point, String> key) {
        final Map<String, Group> first = new TreeMap<>();
        points.stream().filter(p -> !CLASS.equals(nameOf(p).namedBy()))
            .forEach(p -> first.putIfAbsent(key.apply(p), nameOf(p)));
        return first;
    }

    // A call, by its method and line: one its callers give named addresses is listed once for each, an unknown one too.
    private static String callOf(final Facts.Point point) {
        return point.getMethod() + ":" + point.getLine();
    }

    private static Group nameOf(final Facts.Point point) {
        final Address address = point.getAddress();
        return hostOf(address).map(host -> new Group(host, "host", new ArrayList<>()))
            .orElseGet(() -> address.isNamed() ? new Group(address.getValue(), address.getSource(), new ArrayList<>())
                : new Group(typeOf(point.getMethod()), CLASS, new ArrayList<>()));
    }

    private static String typeOf(final String method) {
        return method.substring(0, method.indexOf('#'));
    }

    // The name an address gives a system: its host, or the setting, variable or JNDI name it comes from.
    static Optional<String> nameOf(final Address address) {
        return hostOf(address).or(() -> address.isNamed() ? Optional.of(address.getValue()) : Optional.empty());
    }

    static Optional<String> hostOf(final Address address) {
        return textOf(address).filter(text -> !address.isPlaceholder())
            .map(text -> HOST.matcher(text.strip()))
            .filter(Matcher::matches)
            .map(matcher -> matcher.group(1))
            .filter(host -> !host.contains("{"));
    }

    private static Optional<String> textOf(final Address address) {
        if (Address.SETTING.equals(address.getSource())) {
            return Optional.ofNullable(address.getSettingValue());
        }
        return address.isLiteral() ? Optional.of(address.getValue()) : Optional.empty();
    }
}
