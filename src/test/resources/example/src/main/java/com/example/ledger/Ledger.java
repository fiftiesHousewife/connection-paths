package com.example.ledger;

import com.example.db.Database;
import java.sql.Connection;

public class Ledger {
    private final Connection connection = new Database().openQuietly();

    public int entries() {
        return connection == null ? 0 : 1;
    }
}
