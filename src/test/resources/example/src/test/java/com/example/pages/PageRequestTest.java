package com.example.pages;

import org.junit.jupiter.api.Test;

class PageRequestTest {
    @Test
    void fetchesAPage() {
        PageRequest.connect("https://pages.internal/home").get();
    }
}
