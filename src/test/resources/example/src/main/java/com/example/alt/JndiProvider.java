package com.example.alt;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class JndiProvider implements ConnectionProvider {
    private final InitialContext context;

    public JndiProvider(InitialContext context) {
        this.context = context;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return ((DataSource) context.lookup("java:comp/env/jdbc/app")).getConnection();
        } catch (NamingException e) {
            throw new SQLException(e);
        }
    }
}
