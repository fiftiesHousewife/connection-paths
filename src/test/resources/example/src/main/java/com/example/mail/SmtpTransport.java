package com.example.mail;

import javax.mail.Transport;

public class SmtpTransport extends Transport {
    @Override
    public void sendMessage(String message, String[] addresses) {
    }
}
