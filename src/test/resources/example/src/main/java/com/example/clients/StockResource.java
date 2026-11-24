package com.example.clients;

import javax.ws.rs.GET;

public class StockResource {
    @GET
    public int count() {
        return 3;
    }
}
