package com.example.orders;

public final class OrderStores {
    private OrderStores() {
    }

    public static OrderStore inProduction() {
        return new JdbcOrderStore();
    }
}
