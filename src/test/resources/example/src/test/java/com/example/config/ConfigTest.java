package com.example.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConfigTest {
    @Test
    void readsASetting() {
        assertEquals("name", Settings.get("name"));
    }

    @Test
    void sizesThePool() {
        assertEquals(0, Pool.SHARED.size());
    }
}
