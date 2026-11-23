package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OrderServiceTest {
    private final OrderService service = new OrderService(new MemoryOrderStore());

    @Test
    public void countsOrders() {
        assertEquals(1, service.total());
    }

    @Test
    public void formatsAnOrder() {
        assertEquals("Order 7", service.format("7"));
    }
}
