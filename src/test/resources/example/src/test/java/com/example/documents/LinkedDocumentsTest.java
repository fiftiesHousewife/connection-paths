package com.example.documents;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LinkedDocumentsTest {
    @Test
    void followsTheLinkInADocument() throws IOException {
        final Reader reader = new InputStreamReader(getClass().getResourceAsStream("/documents/linked.txt"),
            StandardCharsets.UTF_8);
        LinkedDocuments.follow(LinkedDocuments.linkIn(reader));
    }
}
