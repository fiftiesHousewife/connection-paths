package com.example.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StoresTest {
    @Test
    void namesTheStore() {
        assertEquals("orders", new NamedStore().name);
    }

    @Test
    void sizesTheCache() {
        assertEquals(10, new CachedStore().size());
    }
}
