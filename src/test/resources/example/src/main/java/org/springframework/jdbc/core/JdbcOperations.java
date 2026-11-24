package org.springframework.jdbc.core;

/** Stands in for Spring's interface, which the example repo has no classpath for. */
public interface JdbcOperations {
    int queryForInt(String sql);
}
