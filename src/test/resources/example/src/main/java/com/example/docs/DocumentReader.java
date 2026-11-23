package com.example.docs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class DocumentReader {
    public String read(String text) {
        return text.trim();
    }

    public String read(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            return new String(in.readAllBytes());
        }
    }
}
