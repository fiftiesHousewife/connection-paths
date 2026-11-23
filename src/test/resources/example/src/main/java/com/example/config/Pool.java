package com.example.config;

import com.example.db.Database;
import java.sql.Connection;

public enum Pool {
    SHARED(new Database().openQuietly());

    private final Connection connection;

    Pool(Connection connection) {
        this.connection = connection;
    }

    public int size() {
        return connection == null ? 0 : 1;
    }
}
