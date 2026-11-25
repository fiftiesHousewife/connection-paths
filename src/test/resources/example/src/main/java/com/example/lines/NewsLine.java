package com.example.lines;

import java.io.IOException;

public class NewsLine {
    public static void read() throws IOException {
        new LineClient().connect("news.internal");
    }
}
