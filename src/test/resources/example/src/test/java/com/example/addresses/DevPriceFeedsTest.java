package com.example.addresses;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class DevPriceFeedsTest {
    @Autowired
    private PriceFeeds feeds;

    @Test
    void readsTheDevRates() throws IOException {
        feeds.rates();
    }
}
