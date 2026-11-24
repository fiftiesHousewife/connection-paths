package com.example.boot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class LedgerEntries {
    public Connection open() throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://ledger-entries.internal/ledger");
    }
}
