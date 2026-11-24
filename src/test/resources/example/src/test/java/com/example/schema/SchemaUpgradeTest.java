package com.example.schema;

import org.junit.jupiter.api.Test;

class SchemaUpgradeTest {
    @Test
    void upgrades() throws Exception {
        new SchemaUpgrade().upgrade();
    }
}
