package com.example.lines;

import java.io.IOException;

public class WeatherLine {
    public void fetch() throws IOException {
        new LineClient().connect("weather.internal");
    }
}
