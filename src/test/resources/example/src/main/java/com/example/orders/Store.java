package com.example.orders;

public interface Store<T> {
    void save(T item);
}
