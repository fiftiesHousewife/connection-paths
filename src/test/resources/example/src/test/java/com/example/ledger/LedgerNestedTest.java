package com.example.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LedgerNestedTest {
    private Ledger ledger;

    @BeforeEach
    void openTheLedger() {
        ledger = new Ledger();
    }

    @Nested
    class WhenEmpty {
        @Test
        void hasNoEntries() {
            assertEquals(0, ledger.entries());
        }
    }
}
