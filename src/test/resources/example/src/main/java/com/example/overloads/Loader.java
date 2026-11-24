package com.example.overloads;

import java.io.IOException;
import java.net.URL;

public interface Loader {
    String load(String name);

    String load(URL url) throws IOException;
}
