package com.example.events;

import java.util.ArrayList;
import java.util.List;

public class Events {
    private final List<Listener> listeners = new ArrayList<>();

    public void register(Listener listener) {
        listeners.add(listener);
    }

    public void fire(String what) {
        for (Listener listener : listeners) {
            listener.changed(what);
        }
    }
}
