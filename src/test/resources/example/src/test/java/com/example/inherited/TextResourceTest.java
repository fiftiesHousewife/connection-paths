package com.example.inherited;

import org.junit.jupiter.api.BeforeEach;

class TextResourceTest extends ResourceContractTest {
    @BeforeEach
    void createResource() {
        resource = "text";
    }
}
