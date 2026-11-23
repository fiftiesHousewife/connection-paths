package com.example.orders;

public class Archiver {
    private final Store<String> store;

    public Archiver(Store<String> store) {
        this.store = store;
    }

    public void archive(String order) {
        store.save(order);
    }
}
