package io.github.fiftieshousewife.connectionpaths.address;

import com.google.common.net.InetAddresses;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Whether an address stays on the machine: an in-memory or file database, a file, a jar or a classpath resource,
 * the machine's own host name, an address of its own to listen on, or, for a host look-up, a numeric address, which
 * nothing looks up. A connection like that needs no stand-in system. A look-up on the machine's DNS servers is not
 * local: they are other machines.
 */
public final class Locality {

    // A prefix, or pattern, and why an address matching it is local.
    private record Local(Pattern pattern, String because) {
    }

    private static final List<Local> LOCAL = List.of(
        rule("jdbc:hsqldb:(?!hsqls?://|https?://).*", "an HSQLDB database in memory or in a file"),
        rule("jdbc:h2:(?!tcp:|ssl:).*", "an H2 database in memory or in a file"),
        rule("jdbc:derby:(?!//).*", "a Derby database in a file or in memory"),
        rule("jdbc:sqlite:.*", "an SQLite database in a file"),
        rule("(file|jar|classpath):.*", "a file, a jar or a classpath resource"),
        rule("(jdbc:)?([a-z][a-z0-9+.-]*:(//)?)?([^@/]*@)?(localhost|127\\.0\\.0\\.1|\\[?::1]?|0\\.0\\.0\\.0)([:/].*)?",
            "this machine"));

    private static final Map<String, String> LOCAL_SOURCES = Map.of(Address.CLASSPATH_RESOURCE, "a classpath resource",
        Address.LOCAL_FILE, "a file");

    private Locality() {
    }

    private static Local rule(final String regex, final String because) {
        return new Local(Pattern.compile(regex), because);
    }

    private static final Pattern PATH = Pattern.compile("\\.{0,2}/[^/].*");

    private static final Pattern LISTEN_WORDS = Pattern.compile("interface|bind|listen", Pattern.CASE_INSENSITIVE);

    /** Why an address is local, or empty if it may reach another machine. */
    public static Optional<String> of(final Address address, final String kind) {
        final Optional<String> lookup = "host-lookup".equals(kind) ? ofLookup(address) : Optional.empty();
        return lookup.or(() -> isPathOpened(address, kind) ? Optional.of("a file or a classpath resource")
            : of(address));
    }

    // A URL opened on a path alone, such as /config/app.json: a file or a resource on this machine.
    private static boolean isPathOpened(final Address address, final String kind) {
        return "url".equals(kind) && address.isLiteral() && PATH.matcher(address.getValue()).matches();
    }

    // A look-up of an address of this machine to listen on, or of a numeric address (IPv4, or IPv6 in hexadecimal and
    // colons), which nothing looks up.
    private static Optional<String> ofLookup(final Address address) {
        if (isListenSetting(address)) {
            return Optional.of("an address of this machine, to listen on");
        }
        return valueOf(address).filter(Locality::isNumeric).map(v -> "a numeric address, which nothing looks up");
    }

    // An IPv4 or IPv6 address, as an address or in brackets as a URL has it; Guava reads it without looking it up.
    private static boolean isNumeric(final String value) {
        return InetAddresses.isInetAddress(value) || InetAddresses.isUriInetAddress(value);
    }

    // A setting such as network.interface or server.bind-address.
    private static boolean isListenSetting(final Address address) {
        return Address.SETTING.equals(address.getSource()) && LISTEN_WORDS.matcher(address.getValue()).find();
    }

    private static Optional<String> of(final Address address) {
        final Optional<String> bySource = Optional.ofNullable(LOCAL_SOURCES.get(address.getSource()));
        if (bySource.isPresent()) {
            return bySource;
        }
        if (Address.SETTING.equals(address.getSource())) {
            return ofValue(address.getSettingValue());
        }
        return address.isLiteral() ? ofValue(address.getValue()) : Optional.empty();
    }

    // The address's value, from its setting where it is one.
    private static Optional<String> valueOf(final Address address) {
        if (Address.SETTING.equals(address.getSource())) {
            return Optional.ofNullable(address.getSettingValue()).map(String::strip);
        }
        return address.isLiteral() ? Optional.of(address.getValue().strip()) : Optional.empty();
    }

    private static Optional<String> ofValue(final String value) {
        if (value == null) {
            return Optional.empty();
        }
        final String text = value.strip().toLowerCase(Locale.ROOT);
        return LOCAL.stream().filter(l -> l.pattern().matcher(text).matches()).map(Local::because).findFirst();
    }
}
