package com.example.events;

import java.util.List;

public final class StandardListeners {
    private StandardListeners() {
    }

    public static List<Listener> all() {
        return List.of(new AuditListener(), new CacheListener(), new MetricsListener(), new SearchListener());
    }
}
