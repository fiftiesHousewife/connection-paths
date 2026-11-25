package io.github.fiftieshousewife.connectionpaths.facts;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Whether a test's reach connects, taking the first that holds: it runs a mock on the way, so it can't; the test starts
// a server on this machine that stands in for that kind of connection, so it can't; no setting the test runs with
// gives the address, or the address is on this machine, so it can't; the address is a host, so it does; otherwise the
// address is known only at run time, so it may.
final class Grades {

    static final String REACHES = "reaches";
    static final String MAY_REACH = "may-reach";
    static final String CANNOT_REACH = "cannot-reach";

    private static final List<String> HTTP_SERVER_PACKAGES = List.of("org.eclipse.jetty.server.", "io.undertow.");
    private static final List<String> NETTY_PACKAGES = List.of("io.netty.", "reactor.netty.");
    private static final Set<String> HTTP_KINDS = Set.of("http", "url");
    private static final Map<String, List<String>> SERVER_NAMES = Map.of(
        "directory", List.of("ldap"),
        "mail", List.of("greenmail", "mail"),
        "http", List.of("wiremock", "mockwebserver"),
        "url", List.of("wiremock", "mockwebserver"),
        "database", List.of("postgres", "mysql", "mongo", "h2", "database"),
        "queue", List.of("kafka", "activemq", "artemis", "rabbit"));

    // A grade, the reason for it, and that reason in plain English.
    record Grade(String grade, String reason, String text) {
    }

    private Grades() {
    }

    static Grade of(final TestFacts.Reach reach, final List<String> standIns) {
        if (reach.getThroughMock() != null) {
            return new Grade(CANNOT_REACH, "through-a-mock", "it calls a mock first, at " + reach.getThroughMock());
        }
        final Optional<String> server = standIns.stream().filter(s -> standsInFor(s, reach.getKind())).findFirst();
        if (server.isPresent()) {
            return new Grade(CANNOT_REACH, "embedded-server", "the test starts " + server.get()
                + " on this machine in its place");
        }
        return ofItsAddress(reach);
    }

    private static Grade ofItsAddress(final TestFacts.Reach reach) {
        return switch (reach.getAddressForTests()) {
            case TestAddresses.NONE -> new Grade(CANNOT_REACH, "no-address-for-tests",
                "no setting the test runs with gives its address");
            case TestAddresses.LOCAL -> new Grade(CANNOT_REACH, "local", "its address is on this machine");
            case TestAddresses.HOST -> new Grade(REACHES, "host", "its address is a host"
                + Optional.ofNullable(reach.getHost()).map(host -> ", " + host).orElse(""));
            default -> new Grade(MAY_REACH, "run-time", "its address is known only at run time");
        };
    }

    // A server a test starts that takes the place of a kind of connection: by its name, or for a web server, its
    // package, as a simple name such as Server says too little.
    static boolean standsInFor(final String standIn, final String kind) {
        final String simple = standIn.substring(standIn.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        final boolean byName = SERVER_NAMES.getOrDefault(kind, List.of()).stream().anyMatch(simple::contains);
        return byName || HTTP_KINDS.contains(kind) && isAWebServer(standIn, simple);
    }

    private static boolean isAWebServer(final String standIn, final String simple) {
        return HTTP_SERVER_PACKAGES.stream().anyMatch(standIn::startsWith)
            || simple.contains("server") && NETTY_PACKAGES.stream().anyMatch(standIn::startsWith);
    }
}
