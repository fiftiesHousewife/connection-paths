package com.example.events;

public class SearchListener implements Listener {
    private String seen;

    @Override
    public void changed(String what) {
        seen = what;
    }
}
