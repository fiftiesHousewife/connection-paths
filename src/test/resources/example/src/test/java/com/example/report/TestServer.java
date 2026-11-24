package com.example.report;

import org.eclipse.jetty.server.Server;

final class TestServer {
    private TestServer() {
    }

    static void start() {
        new Server(8080).start();
    }
}
