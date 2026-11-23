package com.example.orders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class OrderStoreMockTest {
    private final JdbcOrderStore store = mock(JdbcOrderStore.class);

    @Test
    void stubsTheStore() {
        when(store.all()).thenReturn(List.of("7"));
        verify(store).all();
    }
}
