package com.example.spring;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("classpath:applicationContext.xml")
class ReportsContextTest {
    @Test
    void loadsTheReportBeans() {
    }
}
