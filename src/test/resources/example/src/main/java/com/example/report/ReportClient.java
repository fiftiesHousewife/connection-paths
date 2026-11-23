package com.example.report;

import java.io.IOException;
import java.net.URL;

public class ReportClient {
    public void fetch() {
        try {
            new URL("https://reports.internal/daily").openConnection();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
