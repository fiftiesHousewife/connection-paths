package com.example.alt;

import org.junit.jupiter.api.Test;

class DbManagerTest {
    @Test
    void connects() throws Exception {
        new DbManager(new DefaultProvider()).connect();
    }
}
