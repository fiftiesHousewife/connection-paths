package com.example.lookups;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class TimeClient {
    public String ask(InetAddress server, int port) throws IOException {
        try (Socket socket = new Socket(server, port)) {
            return socket.toString();
        }
    }
}
