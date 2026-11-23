package com.example.addresses;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Fetcher {
    private String target = "http://undefined/";

    public Fetcher target(String url) {
        this.target = url;
        return this;
    }

    public InputStream fetch() throws IOException {
        return new URL(target).openStream();
    }
}
