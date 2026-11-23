package com.example.legacy;

import com.example.events.AuditListener;
import junit.framework.TestCase;

public class AuditTest extends TestCase {
    public void testAudits() {
        new AuditListener().changed("login");
    }

    public void helperNotATest(String name) {
        assertNotNull(name);
    }
}
