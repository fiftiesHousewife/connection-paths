package com.example.documents;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

// A document whose first line links to another, which is fetched.
public final class LinkedDocuments {
    private LinkedDocuments() {
    }

    public static String linkIn(final Reader reader) throws IOException {
        return new BufferedReader(reader).readLine();
    }

    public static InputStream follow(final String link) throws IOException {
        return new URL(link).openStream();
    }
}
