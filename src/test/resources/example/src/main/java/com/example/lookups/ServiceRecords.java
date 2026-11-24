package com.example.lookups;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

public class ServiceRecords {
    public Attributes find(String domain) throws NamingException {
        return context().getAttributes("_chat._tcp." + domain, new String[] {"SRV"});
    }

    // A download in the same class, which DNS has nothing to do with.
    public InputStream zoneFile() throws IOException {
        return new URL("https://zones.internal/chat.zone").openStream();
    }

    public InitialDirContext context() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        return new InitialDirContext(env);
    }
}
