package com.example.watch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WatchersTest {
    @Test
    void countsTheWatchers() {
        assertEquals(1, Watchers.count());
    }

    @Test
    void firesAChange() {
        Watchers.fire("name");
    }
}
