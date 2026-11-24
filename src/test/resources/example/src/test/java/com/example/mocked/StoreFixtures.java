package com.example.mocked;

import static org.mockito.Mockito.mock;

import com.example.orders.JdbcOrderStore;

final class StoreFixtures {
    private StoreFixtures() {
    }

    static JdbcOrderStore mockStore() {
        return mock(JdbcOrderStore.class);
    }
}
