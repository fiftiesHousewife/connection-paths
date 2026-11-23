package com.example.addresses;

import java.io.IOException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Value;

public class WarehouseClient {
    @Value("${warehouse.url}")
    private String url;

    public void stock() throws IOException {
        new URL(url).openConnection();
    }
}
