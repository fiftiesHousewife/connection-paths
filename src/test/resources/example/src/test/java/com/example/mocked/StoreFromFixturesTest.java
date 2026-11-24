package com.example.mocked;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.orders.OrderService;
import org.junit.jupiter.api.Test;

class StoreFromFixturesTest {
    @Test
    void totalsTheOrders() {
        assertEquals(0, new OrderService(StoreFixtures.mockStore()).total());
    }
}
