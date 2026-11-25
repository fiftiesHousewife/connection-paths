package com.example.transfers;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class LocalFileServerTest {
    @Test
    void startsAndStops() throws IOException {
        new LocalFileServer(3021).stop();
    }
}
