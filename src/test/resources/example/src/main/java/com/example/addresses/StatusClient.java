package com.example.addresses;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

// A field, and a local variable of the same name in another method that has nothing to do with it.
public class StatusClient {
    private final String endpoint;

    public StatusClient() {
        this.endpoint = "https://status.internal/health";
    }

    public String describe() {
        String endpoint = "https://docs.internal/status";
        return "See " + endpoint;
    }

    public InputStream check() throws IOException {
        return new URL(endpoint).openStream();
    }
}
