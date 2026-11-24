package com.example.jdbc;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface CustomerRepository extends Repository<String, Long> {
    List<String> findByName(String name);
}
