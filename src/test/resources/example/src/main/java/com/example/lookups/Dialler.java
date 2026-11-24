package com.example.lookups;

import java.io.IOException;
import java.net.Socket;

public class Dialler {
    public Socket open() throws IOException {
        try {
            return new Socket("primary.example.com", 5269);
        } catch (IOException e) {
            return new Socket("backup.example.com", 5269);
        }
    }
}
