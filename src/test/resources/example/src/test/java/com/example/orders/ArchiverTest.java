package com.example.orders;

import org.junit.jupiter.api.Test;

class ArchiverTest {
    @Test
    void archives() {
        new Archiver(new OrderArchive()).archive("7");
    }
}
