package com.example.transfers;

import java.io.IOException;
import java.net.Socket;

// A client that connects to a file server, then opens a second connection, to a host the server's reply names, for
// each listing.
public class FileServerClient {
    private Socket control;
    private String dataHost;

    public void connect(String host) throws IOException {
        connect(host, 21);
    }

    public void connect(String host, int port) throws IOException {
        control = new Socket(host, port);
        dataHost = readReply();
    }

    public String list() throws IOException {
        try (Socket data = new Socket(dataHost, 20)) {
            return data.toString();
        }
    }

    private String readReply() {
        return control.toString();
    }
}
