package com.example.mail;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MailLoginWithGreenMailTest {
    @RegisterExtension
    static final GreenMailExtension MAIL = new GreenMailExtension();

    @Test
    void checksAUser() {
        assertTrue(new MailLogin().check("someone", "secret"));
    }
}
