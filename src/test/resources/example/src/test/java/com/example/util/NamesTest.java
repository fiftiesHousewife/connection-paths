package com.example.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

class NamesTest {
    @Test
    void joinsNames() {
        assertEquals("yes", Names.join(new HashMap<>()));
    }
}
