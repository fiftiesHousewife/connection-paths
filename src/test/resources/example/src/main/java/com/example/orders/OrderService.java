package com.example.orders;

public class OrderService {
    private final OrderStore store;

    public OrderService(OrderStore store) {
        this.store = store;
    }

    public int total() {
        return store.all().size();
    }

    public String format(String order) {
        return "Order " + order;
    }
}
