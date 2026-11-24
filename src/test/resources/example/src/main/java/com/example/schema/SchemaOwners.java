package com.example.schema;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SchemaOwners {
    public String firstOwner() throws SQLException, UnknownHostException {
        Connection connection = DriverManager.getConnection(SchemaSettings.get("schema.db.url"));
        ResultSet rows = connection.createStatement().executeQuery("select host from owners");
        String owner = null;
        while (rows.next()) {
            String name = rows.getString(1);
            owner = hostOf(name);
        }
        return owner;
    }

    private static String hostOf(String name) throws UnknownHostException {
        return name + "@" + InetAddress.getLocalHost().getHostName();
    }
}
