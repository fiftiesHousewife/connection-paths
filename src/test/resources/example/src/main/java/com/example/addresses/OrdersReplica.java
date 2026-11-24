package com.example.addresses;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class OrdersReplica {
    private final OrdersDbProperties properties;

    public OrdersReplica(OrdersDbProperties properties) {
        this.properties = properties;
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(properties.getJdbcUrl());
    }
}
