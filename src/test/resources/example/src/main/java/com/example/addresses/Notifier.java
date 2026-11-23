package com.example.addresses;

import java.io.IOException;
import java.net.Socket;

public class Notifier {
    private final String host = System.getenv("NOTIFY_HOST");

    public class Sender {
        public Socket open() throws IOException {
            return new Socket(host, 25);
        }
    }
}
