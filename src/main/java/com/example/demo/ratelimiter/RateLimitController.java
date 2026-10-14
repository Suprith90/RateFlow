package com.example.demo.ratelimiter;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple controller demonstrating rate limiter usage.
 * In a real application, you would apply this to specific endpoints
 * or use AOP/filter-based approaches.
 */
@RestController
public class RateLimitController {

    private final RateLimitService rateLimitService;

    public RateLimitController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/api/resource")
    public ResponseEntity<String> getResource(
            @RequestHeader(value = "X-User-ID", defaultValue = "anonymous") String userId) {

        // Apply rate limit: 10 requests per minute per user
        RateLimitService.Result result = rateLimitService.consumePermission(
                userId,
                "API_CALL",
                10,  // capacity
                10.0 / 60.0  // refill rate: 10 tokens per 60 seconds = 1/6 tokens per second
        );

        if (!result.allowed()) {
            return ResponseEntity.status(429)
                    .header("Retry-After", String.valueOf(result.retryAfterSeconds()))
                    .body("Rate limit exceeded. Try again in " + result.retryAfterSeconds() + " seconds.");
        }

        return ResponseEntity.ok("Resource accessed successfully! Remaining tokens: " + result.remainingTokens());
    }
}