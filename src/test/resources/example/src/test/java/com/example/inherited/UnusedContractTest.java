package com.example.inherited;

import com.example.db.Database;
import org.junit.jupiter.api.Test;

// An abstract test class that no test class extends: its test never runs.
abstract class UnusedContractTest {
    @Test
    void opens() {
        new Database().openQuietly();
    }
}
