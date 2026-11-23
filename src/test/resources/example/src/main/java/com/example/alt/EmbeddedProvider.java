package com.example.alt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class EmbeddedProvider implements ConnectionProvider {
    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:app");
    }
}
