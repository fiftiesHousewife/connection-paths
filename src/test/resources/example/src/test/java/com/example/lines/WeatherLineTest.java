package com.example.lines;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class WeatherLineTest {
    @Test
    void readsTheWeather() throws IOException {
        new WeatherLine().fetch();
    }
}
