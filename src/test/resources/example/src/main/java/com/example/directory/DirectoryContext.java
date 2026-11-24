package com.example.directory;

import java.util.Hashtable;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

public class DirectoryContext extends InitialDirContext {
    public DirectoryContext(Hashtable<?, ?> environment) throws NamingException {
        super(environment);
    }
}
