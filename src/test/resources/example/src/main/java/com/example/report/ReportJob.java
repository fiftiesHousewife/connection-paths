package com.example.report;

public class ReportJob {
    private final ReportClient client = new ReportClient();

    public void run() {
        Runnable fetch = () -> client.fetch();
        fetch.run();
    }
}
