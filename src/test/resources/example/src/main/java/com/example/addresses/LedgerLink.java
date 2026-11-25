package com.example.addresses;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

// A host whose name ends in a country's domain, given to a JDK method that takes an address: no setting's key.
public class LedgerLink {
    public Socket open() throws IOException {
        return new Socket(InetAddress.getByName("ledger.acme.de"), 5432);
    }
}
