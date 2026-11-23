package com.example.greet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GreetingsTest {
    @Test
    void says() {
        assertEquals("Hello world", new Greetings().say(new RemoteGreeter("greetings.internal")));
    }
}
