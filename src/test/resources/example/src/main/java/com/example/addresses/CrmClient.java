package com.example.addresses;

import com.example.config.Settings;
import java.io.IOException;
import java.net.Socket;

public class CrmClient {
    public Socket open() throws IOException {
        return new Socket(Settings.get("crm.host"), 443);
    }
}
