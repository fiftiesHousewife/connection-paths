package com.example.ledger;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LedgerTestNgTest {
    private Ledger ledger;

    @BeforeMethod
    public void openTheLedger() {
        ledger = new Ledger();
    }

    @Test
    public void countsEntries() {
        assertEquals(ledger.entries(), 0);
    }
}
