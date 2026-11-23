package com.example.events;

public class MetricsListener implements Listener {
    private String seen;

    @Override
    public void changed(String what) {
        seen = what;
    }
}
