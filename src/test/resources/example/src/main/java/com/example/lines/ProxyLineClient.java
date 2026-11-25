package com.example.lines;

import java.io.IOException;
import java.net.Socket;

// A client that goes through a proxy, which only a program of its own creates.
public class ProxyLineClient extends LineClient {
    @Override
    public Socket connect(String host) throws IOException {
        return new Socket("line-proxy.internal", 8080);
    }
}
