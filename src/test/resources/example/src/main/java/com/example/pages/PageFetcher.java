package com.example.pages;

import java.io.IOException;
import java.net.URL;

final class PageFetcher {
    private PageFetcher() {
    }

    static String fetch(PageRequest request) {
        try {
            return new URL(request.url()).openConnection().getContentType();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
