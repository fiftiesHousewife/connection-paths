package com.example.pages;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class PageClient {
    public String get(String url) throws IOException {
        try (InputStream in = new URL(url).openStream()) {
            return new String(in.readAllBytes());
        }
    }
}
