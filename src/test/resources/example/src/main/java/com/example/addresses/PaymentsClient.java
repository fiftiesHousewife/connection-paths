package com.example.addresses;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class PaymentsClient {
    public InputStream balances() throws IOException {
        return new URL(System.getenv("PAYMENTS_URL")).openStream();
    }
}
