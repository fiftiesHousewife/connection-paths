package com.example.events;

import org.junit.jupiter.api.Test;

class EventsTest {
    @Test
    void firesToEveryListener() {
        Events events = new Events();
        events.register(new CacheListener());
        events.fire("price");
    }
}
