package com.example.clients;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "orders", url = "${orders.url}")
public interface OrdersClient {
    List<String> all();
}
