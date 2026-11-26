package com.example.spring;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("classpath:feedsContext.xml")
@ActiveProfiles("dev")
class DevFeedsContextTest {
    @Test
    void loadsTheDevBeans() {
    }
}
