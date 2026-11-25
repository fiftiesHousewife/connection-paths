package com.example.lines;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class LineClientTest {
    @Test
    void connectsToItsOwnHost() throws IOException {
        new LineClient().connect("lines.internal");
    }

    @Test
    void connectsToAHostItIsHanded() throws IOException {
        new LineClient().connect(hosts()[0]);
    }

    private static String[] hosts() {
        return new String[] {System.getenv("LINE_HOSTS")};
    }

    @Test
    void connectsToAHostKnownOnlyAtRunTime() throws IOException {
        new LineClient().connect(System.getProperty("line.host"));
    }
}
