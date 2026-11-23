package com.example.events;

public class CacheListener implements Listener {
    private String seen;

    @Override
    public void changed(String what) {
        seen = what;
    }
}
