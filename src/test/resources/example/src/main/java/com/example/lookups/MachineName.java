package com.example.lookups;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MachineName {
    public String canonicalName() throws UnknownHostException {
        return InetAddress.getLocalHost().getCanonicalHostName();
    }

    public String peerName(Socket socket) {
        return socket.getInetAddress().getHostName();
    }

    public String describe(Socket socket) {
        return "talking to " + peerName(socket);
    }
}
