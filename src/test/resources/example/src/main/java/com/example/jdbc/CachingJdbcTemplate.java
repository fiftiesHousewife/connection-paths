package com.example.jdbc;

import org.springframework.jdbc.core.JdbcOperations;

public class CachingJdbcTemplate implements JdbcOperations {
    @Override
    public int queryForInt(String sql) {
        return 0;
    }

    public void clearCache() {
    }
}
