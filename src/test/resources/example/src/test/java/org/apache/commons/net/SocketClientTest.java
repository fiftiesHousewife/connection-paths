package org.apache.commons.net;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class SocketClientTest {
    private final SocketClient client = new SocketClient();

    @Test
    void connectsToTheMirror() throws IOException {
        client.connect("ftp-mirror.internal", 21);
    }

    @Test
    void connectsOnTheDefaultPort() throws IOException {
        client.connect("ftp-default.internal");
    }
}
