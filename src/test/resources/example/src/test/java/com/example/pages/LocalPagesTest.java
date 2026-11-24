package com.example.pages;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LocalPagesTest {
    private static String base;

    @BeforeAll
    static void start() {
        base = LocalPages.BASE;
    }

    @Test
    void fetchesTheFirstPage() {
        PageRequest.connect(base).get();
    }

    @Test
    void fetchesAPageWithAQuery() {
        PageRequest.connect(base + "?q=first").get();
    }

    @Test
    void fetchesAPageByItsPath() {
        PageRequest.connect(LocalPages.urlTo("/first.html")).get();
    }

    @Test
    void fetchesEachPageInAList() {
        String[] urls = {LocalPages.urlTo("/first.html"), LocalPages.urlTo("/second.html")};
        for (String url : urls) {
            PageRequest.connect(url).get();
        }
    }

    @Test
    void fetchesEachPageByItsPlaceInAList() {
        String[] urls = {LocalPages.urlTo("/first.html"), LocalPages.urlTo("/second.html")};
        for (int i = 0; i < urls.length; i++) {
            PageRequest.connect(urls[i]).get();
        }
    }
}
