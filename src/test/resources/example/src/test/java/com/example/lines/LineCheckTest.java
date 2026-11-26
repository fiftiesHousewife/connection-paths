package com.example.lines;

import java.io.IOException;
import java.net.Socket;
import org.junit.jupiter.api.Test;

class LineCheckTest {
    @Test
    void checksTheLineThenConnects() throws IOException {
        new Socket("lines.internal", 79).close();
        new LineClient().connect("lines.internal");
    }
}
