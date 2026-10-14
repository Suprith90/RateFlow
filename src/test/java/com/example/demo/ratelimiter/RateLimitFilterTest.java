package com.example.demo.ratelimiter;

import com.example.demo.ratelimiter.RateLimitFilter;
import com.example.demo.ratelimiter.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitFilter.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Test
    void shouldApplyRateLimitingBasedOnAuthenticatedUser() throws Exception {
        // Given
        request.setRequestURI("/api/test");
        request.setMethod("GET");
        // Mock authenticated user
        request.setUserPrincipal(() -> "test-user");

        // Mock rate limit service to allow request
        when(rateLimitService.consumePermission(
                eq("user:test-user"),
                eq("DEFAULT"),
                eq(100L),
                eq(10.0)
        )).thenReturn(new RateLimitService.Result(true, 99L, 100L, 0L));

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(rateLimitService).consumePermission(
                eq("user:test-user"),
                eq("DEFAULT"),
                eq(100L),
                eq(10.0)
        );
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldApplyRateLimitingBasedOnIPWhenNoAuthentication() throws Exception {
        // Given
        request.setRequestURI("/api/test");
        request.setMethod("GET");
        // No authentication
        request.setUserPrincipal(null);
        // Mock session ID as invalid
        request.setRequestedSessionId("invalid-session");
        // Mock remote IP
        request.setRemoteAddr("192.168.1.100");

        // Mock rate limit service to allow request
        when(rateLimitService.consumePermission(
                eq("ip:192.168.1.100"),
                eq("DEFAULT"),
                eq(100L),
                eq(10.0)
        )).thenReturn(new RateLimitService.Result(true, 99L, 100L, 0L));

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(rateLimitService).consumePermission(
                eq("ip:192.168.1.100"),
                eq("DEFAULT"),
                eq(100L),
                eq(10.0)
        );
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectRequestWhenRateLimited() throws Exception {
        // Given
        request.setRequestURI("/api/test");
        request.setMethod("GET");
        request.setUserPrincipal(() -> "test-user");

        // Mock rate limit service to reject request
        when(rateLimitService.consumePermission(
                eq("user:test-user"),
                eq("DEFAULT"),
                eq(100L),
                eq(10.0)
        )).thenReturn(new RateLimitService.Result(false, 0L, 100L, 5));

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("5", response.getHeader("Retry-After"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldAllowDifferentPoliciesForSameUser() throws Exception {
        // Given
        request.setRequestURI("/api/expensive");
        request.setMethod("POST");
        request.setUserPrincipal(() -> "test-user");

        // Mock rate limit service for different policies
        when(rateLimitService.consumePermission(
                eq("user:test-user"),
                eq("EXPENSIVE_OPERATION"),
                eq(10L),
                eq(1.0)
        )).thenReturn(new RateLimitService.Result(true, 9L, 10L, 0L));

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(rateLimitService).consumePermission(
                eq("user:test-user"),
                eq("EXPENSIVE_OPERATION"),
                eq(10L),
                eq(1.0)
        );
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldExcludeHealthEndpointsFromRateLimiting() throws Exception {
        // Given
        request.setRequestURI("/actuator/health");
        request.setMethod("GET");

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(rateLimitService, never()).consumePermission(anyString(), anyString(), anyLong(), anyDouble());
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldFailOpenWhenRedisUnavailable() throws Exception {
        // Given
        request.setRequestURI("/api/test");
        request.setMethod("GET");
        request.setUserPrincipal(() -> "test-user");

        // Mock rate limit service to throw exception (Redis unavailable)
        when(rateLimitService.consumePermission(
                anyString(),
                anyString(),
                anyLong(),
                anyDouble()
        )).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(filterChain).doFilter(request, response);
    }
}