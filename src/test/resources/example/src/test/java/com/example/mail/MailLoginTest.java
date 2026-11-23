package com.example.mail;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MailLoginTest {
    @Test
    void checksAUser() {
        assertTrue(new MailLogin().check("someone", "secret"));
    }
}
