package com.example.overloads;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class RemoteLoader implements Loader {
    @Override
    public String load(String name) {
        return name.strip();
    }

    @Override
    public String load(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            return new String(in.readAllBytes());
        }
    }
}
