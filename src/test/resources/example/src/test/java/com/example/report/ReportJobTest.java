package com.example.report;

import org.junit.jupiter.api.Test;

class ReportJobTest {
    @Test
    void runsTheReport() {
        new ReportJob().run();
    }
}
