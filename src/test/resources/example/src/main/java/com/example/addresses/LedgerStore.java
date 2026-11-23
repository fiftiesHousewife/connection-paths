package com.example.addresses;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class LedgerStore {
    public Connection open() throws SQLException {
        return DriverManager.getConnection(Config.xml("database.serverURL"));
    }
}
