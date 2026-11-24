package io.github.fiftieshousewife.connectionpaths;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Whether an address stays on the machine: an in-memory or file database, a file, a jar or a classpath resource,
 * the machine's own host name, an address of its own to listen on, or its DNS servers. A connection like that needs
 * no stand-in system.
 */
final class Locality {

    /** A prefix, or pattern, and why an address matching it is local. */
    private record Local(Pattern pattern, String because) {
    }

    private static final List<Local> LOCAL = List.of(
        rule("jdbc:hsqldb:(?!hsqls?://|https?://).*", "an HSQLDB database in memory or in a file"),
        rule("jdbc:h2:(?!tcp:|ssl:).*", "an H2 database in memory or in a file"),
        rule("jdbc:derby:(?!//).*", "a Derby database in a file or in memory"),
        rule("jdbc:sqlite:.*", "an SQLite database in a file"),
        rule("(file|jar|classpath):.*", "a file, a jar or a classpath resource"),
        rule("([a-z][a-z0-9+.-]*:(//)?)?([^@/]*@)?(localhost|127\\.0\\.0\\.1|\\[?::1]?|0\\.0\\.0\\.0)([:/].*)?",
            "this machine"));

    private Locality() {
    }

    private static Local rule(final String regex, final String because) {
        return new Local(Pattern.compile(regex), because);
    }

    /** A path with no scheme or host: for a URL, a file or a classpath resource. */
    private static final Pattern PATH = Pattern.compile("\\.{0,2}/[^/].*");

    /** Words in a setting's key that say it holds an address of this machine to listen on, not another's. */
    private static final Pattern LISTEN_WORDS = Pattern.compile("interface|bind|listen", Pattern.CASE_INSENSITIVE);

    /** Why an address is local, or empty if it may reach another machine. */
    static Optional<String> of(final Address address, final String kind) {
        if ("host-lookup".equals(kind) && isListenSetting(address)) {
            return Optional.of("an address of this machine, to listen on");
        }
        final boolean path = "url".equals(kind) && address.isLiteral()
            && PATH.matcher(address.getValue()).matches();
        return path ? Optional.of("a file or a classpath resource") : of(address);
    }

    /** A setting such as {@code network.interface} or {@code server.bind-address}. */
    private static boolean isListenSetting(final Address address) {
        return Address.SETTING.equals(address.getSource()) && LISTEN_WORDS.matcher(address.getValue()).find();
    }

    private static Optional<String> of(final Address address) {
        return switch (address.getSource()) {
            case Address.CLASSPATH_RESOURCE -> Optional.of("a classpath resource");
            case Address.LOCAL_FILE -> Optional.of("a file");
            case Address.DNS -> Optional.of("the machine's DNS servers");
            case Address.SETTING -> ofValue(address.getSettingValue());
            default -> address.isLiteral() ? ofValue(address.getValue()) : Optional.empty();
        };
    }

    private static Optional<String> ofValue(final String value) {
        if (value == null) {
            return Optional.empty();
        }
        final String text = value.strip().toLowerCase(Locale.ROOT);
        return LOCAL.stream().filter(l -> l.pattern().matcher(text).matches()).map(Local::because).findFirst();
    }
}
