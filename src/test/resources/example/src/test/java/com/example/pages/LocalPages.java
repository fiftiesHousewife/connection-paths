package com.example.pages;

// Serves pages from this machine, on a port chosen when it starts.
final class LocalPages {
    static final String BASE = map("pages");

    private static int port;

    private LocalPages() {
    }

    static String map(String name) {
        String path = "/" + name;
        return "http://localhost:" + port + path;
    }

    static String urlTo(String path) {
        return BASE + path;
    }
}
