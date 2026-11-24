package com.example.alt;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class LocalThings {
    public InputStream readBundledFile() throws IOException {
        return new URL("file:/opt/app/defaults.txt").openStream();
    }

    public InputStream readOwnJar() throws IOException {
        return LocalThings.class.getProtectionDomain().getCodeSource().getLocation().openStream();
    }

    public Socket talkToTheAgentOnThisMachine() throws IOException {
        return new Socket("localhost", 8126);
    }

    public Connection openTheDatabaseOnThisMachine() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost/shop");
    }
}
