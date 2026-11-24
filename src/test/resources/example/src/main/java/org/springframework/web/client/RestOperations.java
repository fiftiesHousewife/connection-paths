package org.springframework.web.client;

public interface RestOperations {
    <T> T getForObject(String url, Class<T> type);
}
