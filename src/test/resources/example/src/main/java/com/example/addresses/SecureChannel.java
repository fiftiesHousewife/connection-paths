package com.example.addresses;

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import javax.net.ssl.SSLContext;

public class SecureChannel {
    public Socket open() throws IOException, GeneralSecurityException {
        return SSLContext.getInstance("TLSv1.2").getSocketFactory().createSocket();
    }
}
