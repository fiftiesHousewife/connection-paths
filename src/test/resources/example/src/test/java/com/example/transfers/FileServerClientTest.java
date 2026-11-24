package com.example.transfers;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileServerClientTest {
    private final FileServerClient client = new FileServerClient();

    @BeforeEach
    void connect() throws IOException {
        client.connect("files.internal");
    }

    @Test
    void listsTheFiles() throws IOException {
        client.list();
    }
}
