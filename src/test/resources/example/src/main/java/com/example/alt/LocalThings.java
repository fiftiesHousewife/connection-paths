package com.example.alt;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;

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
}
