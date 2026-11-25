package com.example.lines;

import java.io.IOException;

// A program run on its own, never by a test.
public final class ProxyLineTool {
    private ProxyLineTool() {
    }

    public static void main(String[] args) throws IOException {
        new ProxyLineClient().connect(args[0]);
    }
}
