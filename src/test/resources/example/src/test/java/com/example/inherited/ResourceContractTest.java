package com.example.inherited;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

abstract class ResourceContractTest {
    protected Object resource;

    @Test
    void isReady() {
        assertNotNull(resource);
    }
}
