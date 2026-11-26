package com.example.spring;

import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

public class LedgerSpringTestCase extends AbstractDependencyInjectionSpringContextTests {
    @Override
    protected String[] getConfigLocations() {
        return new String[] {"ledgerContext.xml"};
    }

    public void testLoadsTheLedgerBeans() {
    }
}
