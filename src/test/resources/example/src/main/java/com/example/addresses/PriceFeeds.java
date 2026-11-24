package com.example.addresses;

import java.io.IOException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Value;

// Feeds whose addresses are settings: one with a default in the code, one a file fills from an environment variable,
// and one a file fills from an environment variable with a default.
public class PriceFeeds {
    @Value("${pricing.url:https://pricing.internal/api}")
    private String pricing;

    @Value("${inventory.url}")
    private String inventory;

    @Value("${payments.url}")
    private String payments;

    public void prices() throws IOException {
        new URL(pricing).openConnection();
    }

    public void stock() throws IOException {
        new URL(inventory).openConnection();
    }

    public void payments() throws IOException {
        new URL(payments).openConnection();
    }
}
