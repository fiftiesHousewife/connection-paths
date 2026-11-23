package com.example.orders;

import com.example.db.Database;
import java.util.List;

public class JdbcOrderStore implements OrderStore {
    private final Database database = new Database();

    @Override
    public List<String> all() {
        database.openQuietly();
        return List.of();
    }
}
