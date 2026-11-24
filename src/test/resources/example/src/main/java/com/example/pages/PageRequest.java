package com.example.pages;

public class PageRequest {
    private String url = "http://undefined/";

    public static PageRequest connect(String url) {
        PageRequest request = new PageRequest();
        request.url(url);
        return request;
    }

    public PageRequest url(String address) {
        this.url = address;
        return this;
    }

    public String url() {
        return url;
    }

    public String get() {
        return PageFetcher.fetch(this);
    }
}
