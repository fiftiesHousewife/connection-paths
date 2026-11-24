package com.example.plugins;

import org.junit.jupiter.api.Test;

class ExportersTest {
    @Test
    void exports() throws Exception {
        Exporters.configured().export("daily");
    }
}
