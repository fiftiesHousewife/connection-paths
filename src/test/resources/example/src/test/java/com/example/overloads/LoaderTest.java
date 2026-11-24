package com.example.overloads;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import org.junit.jupiter.api.Test;

class LoaderTest {
    private final Loader loader = new RemoteLoader();

    @Test
    void loadsByName() {
        assertEquals("a", loader.load(" a "));
    }

    @Test
    void loadsFromAUrl() throws Exception {
        assertEquals("", loader.load(new URL("https://files.internal/a")));
    }
}
