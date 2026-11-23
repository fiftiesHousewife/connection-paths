package com.example.addresses;

import java.util.Properties;

public class DirectoryProperties {
    private static final String SERVER_URL = "directory.server.url";
    private final Properties props = new Properties();

    public String serverUrl() {
        String url = props.getProperty(SERVER_URL);
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url;
    }

    public long timeout() {
        return Long.parseLong(props.getProperty("directory.timeout"));
    }
}
