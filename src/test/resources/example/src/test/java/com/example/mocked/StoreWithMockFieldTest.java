package com.example.mocked;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.orders.OrderService;
import com.example.orders.OrderStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class StoreWithMockFieldTest {
    @Mock
    private OrderStore store;

    @Test
    void totalsTheOrders() {
        assertEquals(0, new OrderService(store).total());
    }
}
