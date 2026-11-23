package com.example.stores;

import com.example.db.Database;

public class BaseStore {
    protected final String name;

    public BaseStore(String name) {
        this.name = name;
        new Database().openQuietly();
    }
}
