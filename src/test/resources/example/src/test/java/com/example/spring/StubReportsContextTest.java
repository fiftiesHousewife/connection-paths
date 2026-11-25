package com.example.spring;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("classpath:spring/applicationContext.xml")
class StubReportsContextTest {
    @Test
    void loadsTheStubBeans() {
    }
}
