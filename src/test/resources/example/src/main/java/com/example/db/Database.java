package com.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    public Connection open() throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://orders-db.internal/orders");
    }

    public Connection openQuietly() {
        try {
            return open();
        } catch (SQLException e) {
            return null;
        }
    }
}
