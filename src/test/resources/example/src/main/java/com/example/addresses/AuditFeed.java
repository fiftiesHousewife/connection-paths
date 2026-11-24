package com.example.addresses;

import com.example.config.Settings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class AuditFeed {
    public int read() throws IOException {
        try (InputStream in = new URL(Settings.get("audit.feed.url")).openStream()) {
            return in.read();
        }
    }
}
