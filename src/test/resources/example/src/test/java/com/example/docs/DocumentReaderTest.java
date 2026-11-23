package com.example.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DocumentReaderTest {
    @Test
    void readsText() {
        assertEquals("<a/>", new DocumentReader().read(" <a/> "));
    }
}
