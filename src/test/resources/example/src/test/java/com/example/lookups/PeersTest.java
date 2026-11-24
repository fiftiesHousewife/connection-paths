package com.example.lookups;

import org.junit.jupiter.api.Test;

class PeersTest {
    @Test
    void findsAPeerByItsAddress() throws Exception {
        new Peers().byAddress("192.168.1.20");
    }
}
