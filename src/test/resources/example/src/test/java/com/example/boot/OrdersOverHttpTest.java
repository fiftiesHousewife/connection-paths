package com.example.boot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.web.client.RestOperations;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OrdersOverHttpTest {
    @Autowired
    private RestOperations rest;

    @Test
    void showsAnOrder() {
        rest.getForObject("/orders/1", String.class);
    }
}
