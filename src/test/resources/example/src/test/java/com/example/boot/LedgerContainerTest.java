package com.example.boot;

import org.junit.jupiter.api.Test;

class LedgerContainerTest extends ContainerTestBase {
    @Test
    void storesAnEntry() throws Exception {
        new LedgerEntries().open();
    }
}
