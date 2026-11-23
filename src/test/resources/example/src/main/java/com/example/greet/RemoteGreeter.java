package com.example.greet;

import com.example.db.Database;

public record RemoteGreeter(String host) implements Greeter {
    @Override
    public String greet(String name) {
        new Database().openQuietly();
        return "Hello " + name;
    }
}
