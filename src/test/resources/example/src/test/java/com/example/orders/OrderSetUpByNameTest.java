package com.example.orders;

import com.example.db.Database;
import org.junit.jupiter.api.Test;

class OrderSetUpByNameTest {
    // Named setUp, but with no @BeforeEach: JUnit 5 never runs it before a test.
    void setUp() {
        new Database().openQuietly();
    }

    @Test
    void countsNothing() {
    }
}
