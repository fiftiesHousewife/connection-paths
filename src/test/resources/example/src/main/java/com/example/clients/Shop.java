package com.example.clients;

public class Shop {
    private final OrdersClient orders;
    private final PricesApi prices;
    private final StockResource stock = new StockResource();

    public Shop(OrdersClient orders, PricesApi prices) {
        this.orders = orders;
        this.prices = prices;
    }

    public int orderCount() {
        return orders.all().size();
    }

    public String priceList() {
        return prices.prices();
    }

    public String currency() {
        return prices.currency();
    }

    public int stockCount() {
        return stock.count();
    }
}
