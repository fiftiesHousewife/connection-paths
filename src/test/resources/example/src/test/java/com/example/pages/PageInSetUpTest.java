package com.example.pages;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PageInSetUpTest {
    private String page;

    @BeforeEach
    void fetchThePage() {
        page = PageRequest.connect("https://pages.internal/setup").get();
    }

    @Test
    void hasAPage() {
        page.length();
    }
}
