import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// Custom metrics for detailed analysis
const rateLimit429Rate = new Rate('rate_limit_429_rate');
const requestLatency = new Trend('request_latency');
const notificationCreateLatency = new Trend('notification_create_latency');
const notificationGetLatency = new Trend('notification_get_latency');
const notificationListLatency = new Trend('notification_list_latency');
const errorRate = new Rate('error_rate');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Test data
const TEST_USERS = [
  { id: 'user1', password: 'pass1' },
  { id: 'user2', password: 'pass2' },
  { id: 'user3', password: 'pass3' },
  { id: 'user4', password: 'pass4' },
  { id: 'user5', password: 'pass5' }
];

const NOTIFICATION_TEMPLATES = [
  { id: 'welcome_email', type: 'EMAIL' },
  { id: 'alert_sms', type: 'SMS' },
  { id: 'newsletter_email', type: 'EMAIL' }
];

// Helper function to get random test user
function getRandomUser() {
  return TEST_USERS[Math.floor(Math.random() * TEST_USERS.length)];
}

// Helper function to get random notification template
function getRandomNotificationTemplate() {
  return NOTIFICATION_TEMPLATES[Math.floor(Math.random() * NOTIFICATION_TEMPLATES.length)];
}

// Setup function - runs once before tests
export function setup() {
  // Initialize any test data if needed
  console.log('Performance test setup complete');
  return {};
}

// Main test function for rate-limited API
export function testRateLimitedAPI() {
  const user = getRandomUser();

  // For simplicity in this example, we'll use a header-based approach
  // In a real scenario with proper auth, we'd need to login first and get a token
  const params = {
    headers: {
      'X-User-ID': user.id,
      'Content-Type': 'application/json',
    },
    timeout: '30s',
  };

  const url = `${BASE_URL}/api/resource`;

  const startTime = Date.now();
  const res = http.get(url, params);
  const endTime = Date.now();

  const latency = endTime - startTime;
  requestLatency.add(latency);

  // Check response
  const success = check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
    'response time < 500ms': (r) => latencylatency < 500,
  });

  if (res.status === 429) {
    rateLimit429Rate.add(1);
  } else if (!success) {
    errorRate.add(1);
  }

  // Sleep between requests to simulate realistic user behavior
  sleep(0.1);
}

// Test function for notification creation
export function testNotificationCreation() {
  const user = getRandomUser();
  const template = getRandomNotificationTemplate();

  const payload = {
    type: template.type,
    recipient: template.type === 'EMAIL' ? `test${Date.now()}@example.com` : `+1555${Math.floor(Math.random() * 10000000000)}`,
    templateId: template.id,
    payload: JSON.stringify({
      name: 'Test User',
      action: 'verify_account',
      expiry: '24 hours'
    }),
    idempotencyKey: `idempotent-${Date.now()}-${Math.random()}`
  };

  const params = {
    headers: {
      'Content-Type': 'application/json',
      // In a real scenario with auth, we'd add Authorization header here
      // For now, we'll rely on the Principal extraction mechanism
    },
    timeout: '30s',
  };

  const url = `${BASE_URL}/api/v1/notifications`;

  const startTime = Date.now();
  const res = http.post(url, JSON.stringify(payload), params);
  const endTime = Date.now();

  const latency = endTime - startTime;
  notificationCreateLatency.add(latency);

  // Check response
  const success = check(res, {
    'status is 202': (r) => r.status === 202,
    'response time < 1000ms': (r) => latency < 1000,
    'has valid JSON': (r) => {
      try {
        JSON.parse(r.body);
        return true;
      } catch (e) {
        return false;
      }
    }
  });

  if (!success) {
    errorRate.add(1);
  }

  // Sleep between requests
  sleep(0.5);
}

// Test function for getting a specific notification
export function testGetNotification() {
  // This would require having created notifications first
  // For simplicity, we'll simulate by trying to get a notification
  // In a real test, we would create notifications first and then retrieve them

  const user = getRandomUser();
  const notificationId = Math.floor(Math.random() * 1000) + 1; // Fake ID

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
    timeout: '30s',
  };

  const url = `${BASE_URL}/api/v1/notifications/${notificationId}`;

  const startTime = Date.now();
  const res = http.get(url, params);
  const endTime = Date.now();

  const latency = endTime - startTime;
  notificationGetLatency.add(latency);

  // Check response (we expect 404 for non-existent notifications, which is OK for this test)
  const success = check(res, {
    'status is 200 or 404': (r) => r.status === 200 || r.status === 404,
    'response time < 500ms': (r) => (endTime - startTime) < 500,
  });

  if (!success) {
    errorRate.add(1);
  }

  // Sleep between requests
  sleep(0.2);
}

// Test function for listing notifications
export function testListNotifications() {
  const user = getRandomUser();

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
    timeout: '30s',
  };

  const url = `${BASE_URL}/api/v1/notifications?page=0&size=10`;

  const startTime = Date.now();
  const res = http.get(url, params);
  const endTime = Date.now();

  const latency = endTime - startTime;
  notificationListLatency.add(latency);

  // Check response
  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => (endTime - startTime) < 500,
    'has valid JSON': (r) => {
      try {
        JSON.parse(r.body);
        return true;
      } catch (e) {
        return false;
      }
    }
  });

  if (!success) {
    errorRate.add(1);
  }

  // Sleep between requests
  sleep(0.3);
}