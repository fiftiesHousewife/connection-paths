package com.example.addresses;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

// A URL made from its parts: the protocol's name first, which is no address, then the host and the file.
public class JarEntryReader {
    public InputStream read(String entry) throws IOException {
        return new URL("jar", "", -1, entry).openStream();
    }
}
