package com.example.report;

import org.eclipse.jetty.server.Server;

final class TestServer {
    private static final Server SERVER = new Server(8080);

    private TestServer() {
    }

    static void start() {
        SERVER.start();
    }
}
