package com.example.inherited;

import com.example.ledger.Ledger;
import org.junit.jupiter.api.BeforeEach;

class LedgerResourceTest extends ResourceContractTest {
    @BeforeEach
    void createResource() {
        resource = new Ledger();
    }
}
