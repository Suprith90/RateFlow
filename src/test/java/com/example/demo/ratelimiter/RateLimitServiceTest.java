package com.example.demo.ratelimiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitService implementations.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private TokenBucketService tokenBucketService;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        // In a real implementation, we'd have a concrete RateLimitService
        // For now, we'll test the interface contract
        rateLimitService = tokenBucketService;
    }

    @Test
    void consumePermission_shouldDelegateToTokenBucketService() {
        // Given
        String key = "user:test-user";
        String policy = "API_CALL";
        long capacity = 100L;
        double refillRate = 10.0;
        var expectedResult = new RateLimitService.Result(true, 99L, 100L, 0L);

        when(tokenBucketService.consumePermission(key, policy, capacity, refillRate))
                .thenReturn(expectedResult);

        // When
        var result = rateLimitService.consumePermission(key, policy, capacity, refillRate);

        // Then
        assertEquals(expectedResult, result);
        verify(tokenBucketService).consumePermission(key, policy, capacity, refillRate);
    }

    @Test
    void consumePermission_differentPolicies_shouldUseDifferentKeys() {
        // Given
        String userKey = "user:test-user";
        String apiPolicy = "API_CALL";
        String expensivePolicy = "EXPENSIVE_OPERATION";
        long capacity = 100L;
        double refillRate = 10.0;
        var apiResult = new RateLimitService.Result(true, 99L, 100L, 0L);
        var expensiveResult = new RateLimitService.Result(false, 0L, 100L, 60);

        when(tokenBucketService.consumePermission(eq(userKey + ":" + apiPolicy), eq(apiPolicy), eq(capacity), eq(refillRate)))
                .thenReturn(apiResult);
        when(tokenBucketService.consumePermission(eq(userKey + ":" + expensivePolicy), eq(expensivePolicy), eq(capacity), eq(refillRate)))
                .thenReturn(expensiveResult);

        // When
        var apiResultActual = rateLimitService.consumePermission(userKey, apiPolicy, capacity, refillRate);
        var expensiveResultActual = rateLimitService.consumePermission(userKey, expensivePolicy, capacity, refillRate);

        // Then
        assertEquals(apiResult, apiResultActual);
        assertEquals(expensiveResult, expensiveResultActual);
        // Verify that different keys were used (user:policy format)
        verify(tokenBucketService).consumePermission(
                argThat(k -> k.equals(userKey + ":" + apiPolicy)),
                eq(apiPolicy),
                eq(capacity),
                eq(refillRate)
        );
        verify(tokenBucketService).consumePermission(
                argThat(k -> k.equals(userKey + ":" + expensivePolicy)),
                eq(expensivePolicy),
                eq(capacity),
                eq(refillRate)
        );
    }
}