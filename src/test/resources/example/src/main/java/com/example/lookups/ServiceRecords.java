package com.example.lookups;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

public class ServiceRecords {
    public Attributes find(String domain) throws NamingException {
        return context().getAttributes("_chat._tcp." + domain, new String[] {"SRV"});
    }

    public InitialDirContext context() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        return new InitialDirContext(env);
    }
}
