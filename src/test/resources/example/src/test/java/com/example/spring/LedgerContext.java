package com.example.spring;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.test.context.ContextConfiguration;

@Retention(RetentionPolicy.RUNTIME)
@ContextConfiguration("classpath:ledgerContext.xml")
public @interface LedgerContext {
}
