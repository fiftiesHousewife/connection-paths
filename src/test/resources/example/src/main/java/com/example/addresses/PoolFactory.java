package com.example.addresses;

import java.sql.Connection;
import java.sql.SQLException;

public class PoolFactory {
    public Connection connect(SimpleDataSource source) throws SQLException {
        final SimpleDataSource pool = source;
        pool.setUrl("jdbc:mysql://pool-db.internal/app");
        return pool.getConnection();
    }
}
