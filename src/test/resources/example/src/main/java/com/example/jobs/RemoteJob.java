package com.example.jobs;

import java.io.IOException;
import java.net.URL;

public class RemoteJob extends Job {
    @Override
    public void prepare() {
        try {
            new URL("https://jobs.internal/prepare").openStream().close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
