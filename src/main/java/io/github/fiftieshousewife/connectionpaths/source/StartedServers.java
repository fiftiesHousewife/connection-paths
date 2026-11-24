package io.github.fiftieshousewife.connectionpaths.source;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

// A server a test class's set-up starts on this machine, itself or through helpers in the source, such as an
// embedded Jetty its @BeforeAll starts: a stand-in for the system its tests connect to.
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

    // The server types called into from what runs with the class's tests, following calls in the source.
    private static Set<String> startedBy(final SourceModel model, final String testClass) {
        final Set<MethodKey> seen = new HashSet<>(model.tests().setUpOf(testClass));
        final Deque<MethodKey> next = new ArrayDeque<>(seen);
        final Set<String> servers = new HashSet<>();
        while (!next.isEmpty()) {
            model.getCalls().getOrDefault(next.pop(), Set.of()).forEach(callee -> {
                if (SERVERS.contains(callee.type())) {
                    servers.add(callee.type());
                } else if (seen.add(callee)) {
                    next.push(callee);
                }
            });
        }
        return servers;
    }
}
