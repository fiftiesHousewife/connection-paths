package com.example.addresses;

import javax.sql.DataSource;

public abstract class SimpleDataSource implements DataSource {
    private String url;

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
