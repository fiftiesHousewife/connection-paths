package com.example.spring;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("classpath*:spring/*.xml")
class EverySpringContextTest {
    @Test
    void loadsEveryContextInTheFolder() {
    }
}
