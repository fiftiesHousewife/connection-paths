package com.example.config;

import com.example.db.Database;
import java.sql.Connection;

public final class Settings {
    private static final Connection CONNECTION = new Database().openQuietly();

    private Settings() {
    }

    public static String get(String key) {
        return key;
    }
}
