package com.example.tools;

import java.io.IOException;

public class Shell {
    public void list() throws IOException {
        new ProcessBuilder("ls").start();
    }
}
