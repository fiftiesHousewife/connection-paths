package com.example.addresses;

import com.example.config.Settings;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ListenAddress {
    public InetAddress toListenOn() throws UnknownHostException {
        return InetAddress.getByName(Settings.get("network.interface"));
    }

    public InetAddress partner() throws UnknownHostException {
        return InetAddress.getByName(Settings.get("partner.host"));
    }
}
