package com.example.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public final class LegacyLauncher {
    private LegacyLauncher() {
    }

    public static Object start() {
        return new ClassPathXmlApplicationContext("applicationContext.xml");
    }
}
