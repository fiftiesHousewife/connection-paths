package io.github.fiftieshousewife.connectionpaths.source;

/** Something the source says about a method or a call ({@code caller > callee}) that the call graph needs. */
public enum Mark {
    /** Runs only once a database connection is open. */
    ON_AN_OPEN_DATABASE,
    /** A call that runs only the method it names, as {@code super.m()} does, never a method overriding it. */
    NOT_VIRTUAL
}
