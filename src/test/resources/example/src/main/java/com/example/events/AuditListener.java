package com.example.events;

public class AuditListener implements Listener {
    private String seen;

    @Override
    public void changed(String what) {
        try (java.net.Socket socket = new java.net.Socket("audit.internal", 514)) {
            socket.getOutputStream().write(what.getBytes());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
