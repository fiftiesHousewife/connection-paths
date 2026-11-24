package com.example.alt;

public final class Providers {
    private Providers() {
    }

    public static ConnectionProvider named(String name) {
        switch (name) {
            case "jndi":
                return new JndiProvider();
            case "embedded":
                return new EmbeddedProvider();
            default:
                return new DefaultProvider();
        }
    }
}
