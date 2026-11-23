package com.example.orders;

import java.util.List;

class FailingOrderStore implements OrderStore {
    @Override
    public List<String> all() {
        throw new IllegalStateException("no orders");
    }
}
