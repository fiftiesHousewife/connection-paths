package io.github.fiftieshousewife.connectionpaths.source;

/**
 * Something the source says about a method or a call ({@code caller > callee}) that the call graph needs: that it runs
 * only once a database connection is open, or that a call runs only the method it names, as {@code super.m()} does,
 * never a method overriding it; or that a connection point is none, as a call matching a rule into a client whose code
 * is in the source, which opens the connection itself.
 */
public enum Mark {
    ON_AN_OPEN_DATABASE,
    NOT_VIRTUAL,
    NOT_A_POINT
}
