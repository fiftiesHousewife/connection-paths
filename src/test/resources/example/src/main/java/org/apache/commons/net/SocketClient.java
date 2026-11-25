package org.apache.commons.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.net.SocketFactory;

// A library's own client, whose code is in the source: the connection it opens is in here, not at its callers. Its
// socket comes from a factory that may be set, then connects to the host it is given.
public class SocketClient {
    private SocketFactory factory = SocketFactory.getDefault();
    private Socket socket;

    public void setSocketFactory(SocketFactory socketFactory) {
        factory = socketFactory;
    }

    public void connect(String host) throws IOException {
        connect(host, 21);
    }

    public void connect(String host, int port) throws IOException {
        socket = factory.createSocket();
        socket.connect(new InetSocketAddress(host, port));
    }
}
