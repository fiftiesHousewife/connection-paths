package com.example.clients;

import retrofit2.http.GET;

public interface PricesApi {
    @GET("prices")
    String prices();

    default String currency() {
        return "GBP";
    }
}
