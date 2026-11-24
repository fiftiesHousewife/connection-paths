package com.example.jobs;

public class LocalJob extends Job {
    public static final Job REMOTE = new RemoteJob();

    @Override
    public void prepare() {
        super.prepare();
    }
}
