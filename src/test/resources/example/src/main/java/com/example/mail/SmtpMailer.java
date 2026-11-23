package com.example.mail;

public class SmtpMailer {
    private final SmtpTransport transport = new SmtpTransport();

    public void send(String to) {
        transport.connect("mail.internal", "orders", "secret");
        transport.sendMessage("Your order", new String[] {to});
    }
}
