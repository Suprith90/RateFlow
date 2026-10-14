export const options = {
  // Test configuration for different scenarios
  thresholds: {
    // Custom thresholds for our metrics
    'rate_limit_429_rate': ['rate<0.1'], // Less than 10% 429 responses for normal load
    'request_latency': ['p(95)<500', 'p(99)<1000'], // 95th percentile < 500ms, 99th < 1000ms
    'notification_create_latency': ['p(95)<1000', 'p(99)<2000'],
    'notification_get_latency': ['p(95)<500', 'p(99)<1000'],
    'notification_list_latency': ['p(95)<500', 'p(99)<1000'],
    'error_rate': ['rate<0.05'], // Less than 5% errors
    http_req_failed: ['rate<0.01'], // Less than 1% failed requests
    http_req_duration: ['p(95)<800', 'p(99)<1500'],

    // Additional custom thresholds
    'checks': ['rate>0.99'], // 99% of checks should pass
  },
};