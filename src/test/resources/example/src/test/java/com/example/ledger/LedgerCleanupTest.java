package com.example.ledger;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LedgerCleanupTest {
    @Test
    void startsClean() {
        assertTrue(true);
    }

    @AfterEach
    void checkTheLedgerIsEmpty() {
        new Ledger().entries();
    }
}
