package com.example.spring;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("classpath:ledgerContext.xml")
class LedgerContextTest {
    @Test
    void loadsTheBeansItsContextImports() {
    }
}
