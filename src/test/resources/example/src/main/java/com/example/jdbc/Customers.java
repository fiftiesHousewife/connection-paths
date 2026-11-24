package com.example.jdbc;

import java.util.List;

public class Customers {
    private final CachingJdbcTemplate jdbc;
    private final CustomerRepository repository;

    public Customers(CachingJdbcTemplate jdbc, CustomerRepository repository) {
        this.jdbc = jdbc;
        this.repository = repository;
    }

    public int count() {
        return jdbc.queryForInt("select count(*) from customers");
    }

    public void forget() {
        jdbc.clearCache();
    }

    public List<String> named(String name) {
        return repository.findByName(name);
    }
}
