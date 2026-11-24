package com.example.addresses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AuditFeedTest {
    @Test
    void readsTheFeed() throws Exception {
        assertEquals(-1, new AuditFeed().read());
    }
}
