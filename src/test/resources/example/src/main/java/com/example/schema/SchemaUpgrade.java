package com.example.schema;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SchemaUpgrade {
    public String upgrade() throws SQLException, UnknownHostException {
        Connection connection = DriverManager.getConnection(SchemaSettings.get("schema.db.url"));
        return new SchemaCheck().check(connection);
    }
}
