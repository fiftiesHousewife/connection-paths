package com.example.plugins;

import java.io.IOException;
import java.net.Socket;

public class FtpExporter implements Exporter {
    @Override
    public void export(String report) throws IOException {
        try (Socket socket = new Socket("ftp.internal", 21)) {
            socket.getOutputStream().write(report.getBytes());
        }
    }
}
