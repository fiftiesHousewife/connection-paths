package com.example.directory;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

public class LdapConnector {
    private final List<String> hosts = new ArrayList<>();

    public LdapConnector(Map<String, String> properties) {
        final StringTokenizer tokens = new StringTokenizer(properties.get("ldap.host"), " ,");
        while (tokens.hasMoreTokens()) {
            hosts.add(tokens.nextToken());
        }
    }

    public DirContext connect() throws NamingException {
        final Hashtable<String, Object> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, providerUrl());
        return new DirectoryContext(environment);
    }

    String providerUrl() {
        final StringBuilder url = new StringBuilder();
        hosts.forEach(host -> url.append("ldap://").append(host).append(" "));
        return url.toString().trim();
    }
}
