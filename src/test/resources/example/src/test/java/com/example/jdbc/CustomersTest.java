package com.example.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CustomersTest {
    private final Customers customers = new Customers(new CachingJdbcTemplate(), name -> List.of());

    @Test
    void counts() {
        assertEquals(0, customers.count());
    }

    @Test
    void forgets() {
        customers.forget();
    }

    @Test
    void findsByName() {
        assertEquals(List.of(), customers.named("a"));
    }
}
