package com.example.pages;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PageClientTest {
    @Test
    void fetchesThePageItIsGiven() throws IOException {
        Map<String, String> titles = new HashMap<>();
        titles.get("https://titles.internal/");
        new PageClient().get(LocalPages.BASE);
    }
}
