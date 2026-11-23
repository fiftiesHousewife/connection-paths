package com.example.orders;

import java.util.List;

public class MemoryOrderStore implements OrderStore {
    @Override
    public List<String> all() {
        return List.of("one");
    }
}
