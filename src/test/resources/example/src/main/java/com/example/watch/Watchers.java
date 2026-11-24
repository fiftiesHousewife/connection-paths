package com.example.watch;

import com.example.db.Database;
import java.util.ArrayList;
import java.util.List;

public final class Watchers {
    private static final List<SettingWatcher> ALL = new ArrayList<>();

    static {
        ALL.add(new SettingWatcher() {
            @Override
            public void changed(String key) {
                new Database().openQuietly();
            }
        });
    }

    private Watchers() {
    }

    public static int count() {
        return ALL.size();
    }

    public static void fire(String key) {
        ALL.forEach(watcher -> watcher.changed(key));
    }
}
