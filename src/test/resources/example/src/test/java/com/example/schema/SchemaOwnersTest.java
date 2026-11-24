package com.example.schema;

import org.junit.jupiter.api.Test;

class SchemaOwnersTest {
    @Test
    void findsTheFirstOwner() throws Exception {
        new SchemaOwners().firstOwner();
    }
}
