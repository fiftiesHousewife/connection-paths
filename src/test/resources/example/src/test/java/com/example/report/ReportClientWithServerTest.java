package com.example.report;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ReportClientWithServerTest {
    @BeforeAll
    static void startTheServer() {
        TestServer.start();
    }

    @Test
    void fetches() {
        new ReportClient().fetch();
    }
}
