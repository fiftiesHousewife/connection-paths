package com.example.alt;

import java.sql.Connection;
import java.sql.SQLException;

public class DbManager {
    private final ConnectionProvider provider;

    public DbManager(ConnectionProvider provider) {
        this.provider = provider;
    }

    public Connection connect() throws SQLException {
        return provider.getConnection();
    }
}
