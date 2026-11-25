package com.example.lines;

import java.io.IOException;
import java.net.Socket;

// A client every caller gives its own host.
public class LineClient {
    public Socket connect(String host) throws IOException {
        return new Socket(host, 23);
    }
}
