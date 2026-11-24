package com.example.addresses;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class PriceFeedsTest {
    private final PriceFeeds feeds = new PriceFeeds();

    @Test
    void readsPrices() throws IOException {
        feeds.prices();
    }

    @Test
    void readsStock() throws IOException {
        feeds.stock();
    }

    @Test
    void readsPayments() throws IOException {
        feeds.payments();
    }
}
