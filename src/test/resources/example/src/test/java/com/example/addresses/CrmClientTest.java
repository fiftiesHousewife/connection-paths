package com.example.addresses;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CrmClientTest {
    @Test
    void opens() throws Exception {
        assertNotNull(new CrmClient().open());
    }
}
