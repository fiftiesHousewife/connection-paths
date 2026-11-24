package io.github.fiftieshousewife.connectionpaths.source;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

// A server a test class or its set-up starts on this machine, itself or through helpers in the source, such as an
// embedded Jetty its @BeforeAll starts or a helper's static field holds: a stand-in for the system its tests
// connect to.
final class StartedServers {

    private static final Set<String> SERVERS = Set.of("org.eclipse.jetty.server.Server",
        "com.sun.net.httpserver.HttpServer", "io.undertow.Undertow", "org.apache.catalina.startup.Tomcat",
        "okhttp3.mockwebserver.MockWebServer", "com.github.tomakehurst.wiremock.WireMockServer",
        "org.mockserver.integration.ClientAndServer", "com.icegreen.greenmail.util.GreenMail");

    private StartedServers() {
    }

    static void addTo(final SourceModel model) {
        model.tests().testTypes().forEach(type -> startedBy(model, type)
            .forEach(server -> model.tests().standIn(type, server)));
    }

    // The server types called into from the class's tests and what runs with them, following calls in the source.
    private static Set<String> startedBy(final SourceModel model, final String testClass) {
        final Set<MethodKey> seen = new HashSet<>(model.tests().setUpOf(testClass));
        model.tests().all().keySet().stream().filter(test -> test.type().equals(testClass)).forEach(seen::add);
        final Deque<MethodKey> next = new ArrayDeque<>(seen);
        final Set<String> servers = new HashSet<>();
        while (!next.isEmpty()) {
            model.getCalls().getOrDefault(next.pop(), Set.of()).forEach(callee -> {
                if (SERVERS.contains(callee.type())) {
                    servers.add(callee.type());
                    return;
                }
                // Using a type may run its static initialisers, where a helper often makes its server.
                Stream.of(callee, MethodKey.staticsOf(callee.type())).filter(seen::add).forEach(next::push);
            });
        }
        return servers;
    }
}
