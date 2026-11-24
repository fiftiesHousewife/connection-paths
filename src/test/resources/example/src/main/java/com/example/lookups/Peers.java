package com.example.lookups;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Peers {
    public InetAddress gateway() throws UnknownHostException {
        return InetAddress.getByName("10.0.0.1");
    }

    public InetAddress byAddress(String address) throws UnknownHostException {
        return InetAddress.getByName(address);
    }
}
