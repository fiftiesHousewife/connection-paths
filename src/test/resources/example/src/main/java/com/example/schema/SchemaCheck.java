package com.example.schema;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;

public class SchemaCheck {
    public String check(Connection connection) throws UnknownHostException {
        return "checked from " + InetAddress.getLocalHost().getCanonicalHostName();
    }
}
