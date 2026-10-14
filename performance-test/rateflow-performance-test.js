import { sharedArray } from 'k6/data';
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// Import our test functions
import { testRateLimitedAPI } from './k6-test-script.js';
import { testNotificationCreation } from './k6-test-script.js';
import { testGetNotification } from './k6-test-script.js';
import { testListNotifications } from './k6-test-script.js';

// Custom metrics for detailed analysis
export const rateLimit429Rate = new Rate('rate_limit_429_rate');
export const requestLatency = new Trend('request_latency');
export const notificationCreateLatency = new Trend('notification_create_latency');
export const notificationGetLatency = new Trend('notification_get_latency');
export const notificationListLatency = new Trend('notification_list_latency');
export const errorRate = new Rate('error_rate');
export const redisLatency = new Trend('redis_latency_metrics'); // Would be populated from application metrics
export const dbLatency = new Trend('db_latency_metrics');      // Would be populated from application metrics
export const kafkaLag = new Trend('kafka_consumer_lag');      // Would be populated from application metrics

// Configuration
export const options = {
  thresholds: {
    // Rate limiting thresholds
    'rate_limit_429_rate': ['rate<0.15'], // Allow up to 15% 429s during load testing

    // Latency thresholds
    'request_latency': ['p(95)<500', 'p(99)<1000'],
    'notification_create_latency': ['p(95)<1500', 'p(99)<3000'],
    'notification_get_latency': ['p(95)<500', 'p(99)<1000'],
    'notification_list_latency': ['p(95)<500', 'p(99)<1000'],

    // Error thresholds
    'error_rate': ['rate<0.05'],

    // Standard HTTP thresholds
    'http_req_failed': ['rate<0.01'],
    'http_req_duration': ['p(95)<800', 'p(99)<1500'],

    // Check pass rate
    'checks': ['rate>0.95'],
  },
};

// Test data
export const TEST_USERS = sharedArray('users', function () {
  return [
    { id: 'perf_test_user_1', password: 'testpass1' },
    { id: 'perf_test_user_2', password: 'testpass2' },
    { id: 'perf_test_user_3', password: 'testpass3' },
    { id: 'perf_test_user_4', password: 'testpass4' },
    { id: 'perf_test_user_5', password: 'testpass5' },
    { id: 'perf_test_user_6', password: 'testpass6' },
    { id: 'perf_test_user_7', password: 'testpass7' },
    { id: 'perf_test_user_8', password: 'testpass8' },
    { id: 'perf_test_user_9', password: 'testpass9' },
    { id: 'perf_test_user_10', password: 'testpass10' }
  ];
});

export const NOTIFICATION_TEMPLATES = sharedArray('templates', function () {
  return [
    { id: 'welcome_email', type: 'EMAIL' },
    { id: 'alert_sms', type: 'SMS' },
    { id: 'newsletter_email', type: 'EMAIL' },
    { id: 'promo_sms', type: 'SMS' },
    { id: 'transaction_email', type: 'EMAIL' }
  ];
});

// Base URL - can be overridden via environment variable
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Helper functions
export function getRandomUser() {
  return TEST_USERS[Math.floor(Math.random() * TEST_USERS.length)];
}

export function getRandomNotificationTemplate() {
  return NOTIFICATION_TEMPLATES[Math.floor(Math.random() * NOTIFICATION_TEMPLATES.length)];
}

// Setup function - runs once before tests
export function setup() {
  console.log(`Performance test starting against ${BASE_URL}`);
  console.log('Test users available:', TEST_USERS.length);
  return {};
}

// Teardown function - runs once after tests
export function teardown(data) {
  console.log('Performance test completed');
  // Any cleanup would go here
}

/**
 * Test the rate-limited API endpoint
 * This tests the /api/resource endpoint with rate limiting
 */
export function testRateLimitedAPI() {
  const user = getRandomUser();

  const params = {
    headers: {
      'X-User-ID': user.id,
      'Content-Type': 'application/json',
    },
    timeout: `${__ENV.REQUEST_TIMEOUT || '30s'}`,
  };

  const url = `${BASE_URL}/api/resource`;

  const startTime = Date.now();
  const res = http.get(url, params);
  const endTime = Date.now();

  const latency = endTime - startTime;
  requestLatency.add(latency);

  // Evaluate response
  const statusOK = check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
  });

  if (res.status === 429) {
    rateLimit429Rate.add(1);
  } else if (!statusOK) {
    errorRate.add(1);
  }

  // Additional checks for successful responses
  if (res.status === 200) {
    check(res, {
      'response contains success message': (r) => r.body.includes('Resource accessed successfully'),
      'response time < 500ms': (r) => latency < 500,
    });
  }

  // Sleep between requests to simulate think time
  sleep(__ENV.REQUEST_INTERVAL || 0.1);
}

/**
 * Test notification creation endpoint
 * This tests the POST /api/v1/notifications endpoint
 */
export function testNotificationCreation() {
  const user = getRandomUser();
  const template = getRandomNotificationTemplate();

  // Generate realistic test data
  const recipient = template.type === 'EMAIL'
    ? `perf.${Date.now().toString().substring(-6)}@example.com`
    : `+1555${String(Math.floor(Math.random() * 10000000000)).padStart(10, '0')}`;

  const payload = {
    type: template.type,
    recipient: recipient,
    templateId: template.id,
    payload: JSON.stringify({
      timestamp: Date.now(),
      testId: `perf_test_${Math.floor(Math.random() * 10000)}`,
      message: 'This is a performance test notification'
    }),
    idempotencyKey: `perf_test_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
  };

  const params = {
    headers: {
      'Content-Type': 'application/json',
      // In a real implementation with proper auth, we would add:
      // 'Authorization': `Bearer ${authToken}`
      // For this test, we rely on the X-User-ID header approach used in RateLimitController
      // or we would need to implement proper authentication flow
    },
    timeout: `${__ENV.REQUEST_TIMEOUT || '30s'}`,
  };

  const url = `${BASE_URL}/api/v1/notifications`;

  const startTime = Date.now();
  const res = http.post(url, JSON.stringify(payload), params);
  const endTime = Date.now();

  const latency = endTime - startTime;
  notificationCreateLatency.add(latency);

  // Evaluate response
  const statusOK = check(res, {
    'status is 202': (r) => r.status === 202,
    'response time < 2000ms': (r) => latency < 2000,
    'returns valid JSON': (r) => {
      try {
        const json = JSON.parse(r.body);
        return json && json.id !== undefined;
      } catch (e) {
        return false;
      }
    }
  });

  if (!statusOK) {
    errorRate.add(1);
  }

  // Sleep between requests
  sleep(__ENV.REQUEST_INTERVAL || 0.5);
}

/**
 * Test getting a specific notification
 * This tests the GET /api/v1/notifications/{id} endpoint
 */
export function testGetNotification() {
  const user = getRandomUser();
  // In a real test, we would use IDs from previously created notifications
  // For this test, we'll use a mix of existing and non-existing IDs
  const notificationId = Math.floor(Math.random() * 10000) + 1;

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
    timeout: `${__ENV.REQUEST_TIMEOUT || '30s'}`,
  };

  const url = `${BASE_URL}/api/v1/notifications/${notificationId}`;

  const startTime = Date.now();
  const res = http.get(url, params);
  const endTime = Date.now();

  const latency = endTime - startTime;
  notificationGetLatency.add(latency);

  // For this test, we accept both 200 (found) and 404 (not found) as valid responses
  const statusOK = check(res, {
    'status is 200 or 404': (r) => r.status === 200 || r.status === 404,
    'response time < 800ms': (r) => latency < 800,
  });

  if (!statusOK) {
    errorRate.add(1);
  }

  // Sleep between requests
  sleep(__ENV.REQUEST_INTERVAL || 0.2);
}

/**
 * Test listing notifications
 * This tests the GET /api/v1/notifications endpoint with pagination
 */
export function testListNotifications() {
  const user = getRandomUser();

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
    timeout: `${__ENV.REQUEST_TIMEOUT || '30s'}`,
  };

  const url = `${BASE_URL}/api/v1/notifications?page=0&size=20`;

  const startTime = Date.now();
  const res = http.get(url, params);
  const endTime = Date.now();

  const latency = endTime - startTime;
  notificationListLatency.add(latency);

  // Evaluate response
  const statusOK = check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 800ms': (r) => latency < 800,
    'returns valid JSON with pagination': (r) => {
      try {
        const json = JSON.parse(r.body);
        return json &&
               json.content !== undefined &&
               Array.isArray(json.content) &&
               json.pageNumber !== undefined &&
               json.pageSize !== undefined;
      } catch (e) {
        return false;
      }
    }
  });

  if (!statusOK) {
    errorRate.add(1);
  }

  // Sleep between requests
  sleep(__ENV.REQUEST_INTERVAL || 0.3);
}