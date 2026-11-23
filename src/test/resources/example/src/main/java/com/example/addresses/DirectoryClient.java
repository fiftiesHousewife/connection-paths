package com.example.addresses;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DirectoryClient {
    private final URI server;
    private final long timeout;

    public DirectoryClient() {
        final DirectoryProperties properties = new DirectoryProperties();
        server = URI.create(properties.serverUrl()).resolve("api/");
        timeout = properties.timeout();
    }

    public String users() throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(server.resolve("users"))
            .timeout(Duration.ofMillis(timeout))
            .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}
