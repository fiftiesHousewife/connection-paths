package com.example.orders;

import java.util.List;

class FakeOrderStore implements OrderStore {
    @Override
    public List<String> all() {
        return List.of("1");
    }
}
