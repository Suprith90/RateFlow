package com.example.demo.ratelimiter;

import com.example.demo.metrics.ObservabilityMetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.time.Instant;

/**
 * Spring Filter that implements rate limiting using the TokenBucketService.
 * <p>
 * Features:
 * - Extracts user identity from authenticated principal (Spring Security)
 * - Applies rate limiting to configured API routes
 * - Returns HTTP 429 with Retry-After and rate-limit headers when limited
 * - Uses OncePerRequestFilter to prevent double-counting
 * - Excludes health endpoints by default
 * - Handles Redis unavailability gracefully (fail-open)
 * - Structured JSON error responses
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnProperty(name = "ratelimiter.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final Set<String> DEFAULT_EXCLUDED_PATHS = new HashSet<>(
            Arrays.asList(
                    "/actuator/health",
                    "/actuator/info",
                    "/favicon.ico",
                    "/error"
            )
    );

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;
    private final ObservabilityMetricsService metricsService;

    // Configuration properties - in a real app, these would come from application.properties
    private final long defaultCapacity = 100; // default 100 requests
    private final double defaultRefillRatePerSecond = 10.0; // default 10 req/sec
    private final String defaultPolicy = "DEFAULT";

    @Autowired
    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper,
                           ObservabilityMetricsService metricsService) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
        this.metricsService = metricsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Exclude certain paths by default
        if (DEFAULT_EXCLUDED_PATHS.contains(path)) {
            return true;
        }

        // Exclude OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Additional exclusions can be added here based on configuration
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Determine the rate limit key based on authenticated user or IP address
            String key = resolveRateLimitKey(request);
            String policy = resolveRateLimitPolicy(request);

            // Measure the latency of the rate limit decision
            Instant startTime = Instant.now();

            // Check if request is allowed
            RateLimitService.Result result = rateLimitService.consumePermission(
                    key,
                    policy,
                    defaultCapacity,
                    defaultRefillRatePerSecond
            );

            // Record the latency
            long durationMillis = java.time.Duration.between(startTime, Instant.now()).toMillis();
            metricsService.recordRateLimitDecisionLatency(durationMillis / 1000.0);

            // Add rate limit headers to response
            response.addHeader("X-RateLimit-Limit", Long.toString(result.limit()));
            response.addHeader("X-RateLimit-Remaining", Long.toString(result.remainingTokens()));
            response.addHeader("X-RateLimit-Policy", policy);

            if (!result.allowed()) {
                // Request is rate-limited
                metricsService.recordRateLimitRejected();
                logRateLimitRejection(request, key, policy, result);

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.addHeader("Retry-After", Long.toString(result.retryAfterSeconds()));

                // Structured error response
                RateLimitErrorResponse errorResponse = new RateLimitErrorResponse(
                        "Rate limit exceeded",
                        "Too many requests",
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        result.retryAfterSeconds(),
                        result.limit(),
                        result.remainingTokens()
                );

                objectMapper.writeValue(response.getWriter(), errorResponse);
                return;
            }

            // Request is allowed
            metricsService.recordRateLimitAllowed();

            // Request is allowed, continue with the chain
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // If rate limiting fails due to Redis issues or other problems,
            // we fail open to allow the request through (but log the error)
            log.warn("Rate limiting check failed, allowing request through: {}", e.getMessage(), e);
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Resolves the identity to use for rate limiting.
     * <p>
     * Priority:
     * 1. Authenticated user principal (from Spring Security)
     * 2. Session ID (if available)
     * 3. Client IP address
     * <p>
     * Important: We do NOT use caller-supplied identifiers (like X-User-ID header)
     * as they cannot be trusted for security purposes.
     */
    private String resolveRateLimitKey(HttpServletRequest request) {
        // Try to get authenticated user from Spring Security
        var principal = request.getUserPrincipal();
        if (principal != null && principal.getName() != null && !principal.getName().isEmpty()) {
            return "user:" + principal.getName();
        }

        // Fallback to session ID if available
        String sessionId = request.getRequestedSessionId();
        if (sessionId != null && !sessionId.isEmpty() && !request.isRequestedSessionIdValid()) {
            return "session:" + sessionId;
        }

        // Fallback to client IP address
        String clientIp = getClientIp(request);
        if (clientIp != null && !clientIp.isEmpty()) {
            return "ip:" + clientIp;
        }

        // Last resort: unknown client
        return "unknown";
    }

    /**
     * Resolves the rate limit policy to apply.
     * <p>
     * In a more sophisticated implementation, this could be based on:
     * - User role/permissions
     * - Endpoint being accessed
     * - Subscription tier
     * <p>
     * For now, we use a default policy.
     */
    private String resolveRateLimitPolicy(HttpServletRequest request) {
        // This could be made configurable based on endpoint, user role, etc.
        return defaultPolicy;
    }

    /**
     * Extracts client IP address, considering proxies and load balancers.
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            String[] ips = ip.split(",");
            return ips[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

    /**
     * Logs rate limit rejections for monitoring and debugging.
     */
    private void logRateLimitRejection(HttpServletRequest request, String key, String policy, RateLimitService.Result result) {
        log.info(
                "Rate limit exceeded - Path: {}, Method: {}, Key: {}, Policy: {}, " +
                "Limit: {}, Remaining: {}, Retry-After: {}s",
                request.getRequestURI(),
                request.getMethod(),
                key,
                policy,
                result.limit(),
                result.remainingTokens(),
                result.retryAfterSeconds()
        );
    }

    /**
     * Error response structure for rate limit exceeded responses.
     */
    private static class RateLimitErrorResponse {
        private final String error;
        private final String message;
        private final int status;
        private final long retryAfterSeconds;
        private final long limit;
        private final long remaining;

        public RateLimitErrorResponse(String error, String message, int status, long retryAfterSeconds,
                                    long limit, long remaining) {
            this.error = error;
            this.message = message;
            this.status = status;
            this.retryAfterSeconds = retryAfterSeconds;
            this.limit = limit;
            this.remaining = remaining;
        }

        // Getters for JSON serialization
        public String getError() { return error; }
        public String getMessage() { return message; }
        public int getStatus() { return status; }
        public long getRetryAfterSeconds() { return retryAfterSeconds; }
        public long getLimit() { return limit; }
        public long getRemaining() { return remaining; }
    }
}