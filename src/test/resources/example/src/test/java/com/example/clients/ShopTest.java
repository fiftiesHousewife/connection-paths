package com.example.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ShopTest {
    private final Shop shop = new Shop(List::of, () -> "");

    @Test
    void countsOrders() {
        assertEquals(0, shop.orderCount());
    }

    @Test
    void listsPrices() {
        assertEquals("", shop.priceList());
    }

    @Test
    void namesTheCurrency() {
        assertEquals("GBP", shop.currency());
    }

    @Test
    void countsStock() {
        assertEquals(3, shop.stockCount());
    }
}
