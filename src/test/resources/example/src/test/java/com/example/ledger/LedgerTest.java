package com.example.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LedgerTest {
    private Ledger ledger;

    @BeforeEach
    void createLedger() {
        ledger = new Ledger();
    }

    @Test
    void countsEntries() {
        assertEquals(0, ledger.entries());
    }
}
