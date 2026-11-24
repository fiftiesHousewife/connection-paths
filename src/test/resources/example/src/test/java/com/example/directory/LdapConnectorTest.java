package com.example.directory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import org.junit.jupiter.api.Test;

class LdapConnectorTest {
    @Test
    void connects() throws Exception {
        assertNotNull(new LdapConnector(Map.of("ldap.host", "directory.internal")).connect());
    }
}
