package com.example.report;

import org.junit.jupiter.api.Test;

class ReportClientStartingItsServerTest {
    @Test
    void fetches() {
        TestServer.start();
        new ReportClient().fetch();
    }
}
