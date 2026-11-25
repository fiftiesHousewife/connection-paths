package com.example.transfers;

import java.io.IOException;
import java.net.ServerSocket;

// A server a test starts on this machine, listening on the port it is given.
class LocalFileServer {
    private final ServerSocket socket;

    LocalFileServer(int port) throws IOException {
        socket = new ServerSocket(port);
    }

    void stop() throws IOException {
        socket.close();
    }
}
