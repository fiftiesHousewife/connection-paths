package com.example.lookups;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Peers {
    public InetAddress gateway() throws UnknownHostException {
        return InetAddress.getByName("10.0.0.1");
    }

    // Not an IPv4 address: its numbers are too big, so it is looked up as a name.
    public InetAddress mirror() throws UnknownHostException {
        return InetAddress.getByName("300.1.1.1");
    }

    public InetAddress onTheLink() throws UnknownHostException {
        return InetAddress.getByName("fe80::1%eth0");
    }

    public InetAddress byAddress(String address) throws UnknownHostException {
        return InetAddress.getByName(address);
    }
}
