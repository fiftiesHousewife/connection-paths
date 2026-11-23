package com.example.stores;

import com.example.db.Database;
import java.sql.Connection;

public class BaseCache {
    private final Connection connection = new Database().openQuietly();

    public boolean isOpen() {
        return connection != null;
    }
}
