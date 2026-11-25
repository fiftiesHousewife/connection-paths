package com.example.fetch;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

// A client whose address is set on it before it fetches; made without one, it refuses before it connects.
public class ReportFetcher {
    private String url;

    public ReportFetcher url(String address) {
        this.url = address;
        return this;
    }

    public InputStream fetch() throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("No URL set");
        }
        return new URL(url).openStream();
    }
}
