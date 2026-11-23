package com.example.orders;

import com.example.db.Database;

public class OrderArchive implements Store<String> {
    @Override
    public void save(String order) {
        new Database().openQuietly();
    }
}
