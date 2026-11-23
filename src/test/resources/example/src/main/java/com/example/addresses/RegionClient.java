package com.example.addresses;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class RegionClient {
    public InputStream prices(String region) throws IOException {
        return new URL("https://" + region + ".prices.internal/v1").openStream();
    }
}
