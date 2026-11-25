package com.example.lines;

import java.io.IOException;

public class WeatherLine {
    public static void fetch() throws IOException {
        new LineClient().connect("weather.internal");
    }
}
