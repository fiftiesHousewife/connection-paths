package com.example.addresses;

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

// A plain socket upgraded to TLS: the factory comes from a protocol's name, which is no address.
public class SecureUpgrade {
    public Socket upgrade(Socket plain, String host) throws IOException, GeneralSecurityException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        SSLSocketFactory factory = context.getSocketFactory();
        return factory.createSocket(plain, host, 465, true);
    }
}
