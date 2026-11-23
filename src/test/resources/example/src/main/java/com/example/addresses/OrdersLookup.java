package com.example.addresses;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class OrdersLookup {
    public Connection connect(InitialContext context) throws NamingException, SQLException {
        return ((DataSource) context.lookup("java:comp/env/jdbc/orders")).getConnection();
    }
}
