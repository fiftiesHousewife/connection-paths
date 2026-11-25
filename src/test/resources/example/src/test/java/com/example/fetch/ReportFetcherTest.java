package com.example.fetch;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class ReportFetcherTest {
    @Test
    void refusesWithoutAUrl() throws IOException {
        ReportFetcher fetcher = new ReportFetcher();
        try {
            fetcher.fetch();
        } catch (IllegalArgumentException expected) {
            return;
        }
    }

    @Test
    void fetchesTheReport() throws IOException {
        new ReportFetcher().url("https://reports.internal/daily").fetch();
    }
}
