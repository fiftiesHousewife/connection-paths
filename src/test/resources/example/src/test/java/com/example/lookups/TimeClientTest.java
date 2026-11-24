package com.example.lookups;

import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.Test;

class TimeClientTest {
    @Test
    void asksAServerAtANumericAddress() throws IOException {
        InetAddress here = InetAddress.getByAddress("loopback", new byte[]{127, 0, 0, 1});
        new TimeClient().ask(here, 123);
    }

    @Test
    void asksTheLoopbackAddress() throws IOException {
        new TimeClient().ask(InetAddress.getLoopbackAddress(), 123);
    }
}
