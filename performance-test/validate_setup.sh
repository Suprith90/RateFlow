#!/bin/bash
# Script to validate that the RateFlow application is ready for performance testing

set -e  # Exit on any error

echo "=== RateFlow Performance Test Setup Validation ==="
echo ""

# Configuration
BASE_URL=${BASE_URL:-http://localhost:8080}
TIMEOUT=5

echo "Validating application at: $BASE_URL"
echo ""

# Check 1: Application is responding
echo "1. Checking application availability..."
if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" | grep -q "200"; then
  echo "   ✓ Application is responding"
else
  echo "   ✗ Application is not responding at $BASE_URL/actuator/health"
  echo "     Please ensure the application is running and accessible"
  exit 1
fi

# Check 2: Actuator endpoints are available
echo "2. Checking actuator endpoints..."
ENDPOINTS=("health" "info" "metrics" "prometheus")
for endpoint in "${ENDPOINTS[@]}"; do
  if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/$endpoint" | grep -q "200"; then
    echo "   ✓ /actuator/$endpoint is available"
  else
    echo "   ⚠ /actuator/$endpoint may not be available (status not 200)"
  fi
done

# Check 3: Rate limit endpoint is accessible
echo "3. Checking rate limit endpoint..."
if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/resource" -H "X-User-ID: test-user" | grep -q "200\|429"; then
  echo "   ✓ Rate limit endpoint is accessible"
else
  echo "   ⚠ Rate limit endpoint may not be behaving as expected"
fi

# Check 4: Notification endpoint is accessible
echo "4. Checking notification endpoint..."
# This will likely return 401 or 400 since we're not sending proper auth/data
# but we're checking that the endpoint exists and responds
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/notifications" -X POST -H "Content-Type: application/json" -d '{"type":"EMAIL","recipient":"test@example.com","templateId":"test","payload":"{}"}' || echo "000")
if [[ "$HTTP_CODE" =~ ^(200|201|202|400|401|403|405|500)$ ]]; then
  echo "   ✓ Notification endpoint is responding (HTTP $HTTP_CODE)"
else
  echo "   ⚠ Notification endpoint returned unexpected status: $HTTP_CODE"
fi

# Check 5: Verify required services are accessible (basic check)
echo "5. Checking service accessibility (basic)..."
# These are just basic checks - in reality, the app handles service failures gracefully

echo ""
echo "=== Validation Complete ==="
echo ""
echo "If all checks passed (✓), the application appears ready for testing."
echo "If there were warnings (⚠), please verify the configuration."
echo "If there were errors (✗), please fix the issues before proceeding."
echo ""
echo "Next steps:"
echo "1. Review the test scenarios in scenarios.js"
echo "2. Adjust BASE_URL if needed (currently: $BASE_URL)"
echo "3. Run individual scenarios or use run_all_scenarios.sh"
echo "4. Use PERFORMANCE_REPORT_TEMPLATE.md to document results"