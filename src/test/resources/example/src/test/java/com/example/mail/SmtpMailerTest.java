package com.example.mail;

import org.junit.jupiter.api.Test;

class SmtpMailerTest {
    @Test
    void sends() {
        new SmtpMailer().send("someone@example.com");
    }
}
