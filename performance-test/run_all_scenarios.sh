#!/bin/bash
# Script to run all performance test scenarios sequentially

set -e  # Exit on any error

echo "=== RateFlow Performance Test Suite ==="
echo "Starting at: $(date)"
echo ""

# Configuration
BASE_URL=${BASE_URL:-http://localhost:8080}
RESULTS_DIR="results_$(date +%Y%m%d_%H%M%s)"
mkdir -p "$RESULTS_DIR"

echo "Test configuration:"
echo "  Base URL: $BASE_URL"
echo "  Results directory: $RESULTS_DIR"
echo ""

# Array of scenarios to run
SCENARIOS=(
  "below_rate_limit:5m:10"
  "above_rate_limit:3m:50"
  "multiple_users:7m:20"
  "short_bursts:2m:0"
  "sustained_traffic:10m:25"
  "notification_creation_load:8m:15"
  "notification_processing_lag:15m:30"
)

# Function to run a scenario
run_scenario() {
  local scenario_name=$1
  local duration=$2
  local vus=$3

  echo "Running scenario: $scenario_name"
  echo "  Duration: $duration"
  echo "  VUs: $vus"

  # Special handling for short_bursts scenario
  if [[ "$scenario_name" == "short_bursts" ]]; then
    k6 run \
      --duration $duration \
      --vus $vus \
      --out json="$RESULTS_DIR/${scenario_name}.json" \
      --out csv="$RESULTS_DIR/${scenario_name}.csv" \
      -e BASE_URL="$BASE_URL" \
      ./rateflow-performance-test.js
  else
    k6 run \
      --duration $duration \
      --vus $vus \
      --out json="$RESULTS_DIR/${scenario_name}.json" \
      --out csv="$RESULTS_DIR/${scenario_name}.csv" \
      -e BASE_URL="$BASE_URL" \
      ./rateflow-performance-test.js
  fi

  echo "Completed scenario: $scenario_name"
  echo ""
}

# Run all scenarios
for scenario_def in "${SCENARIOS[@]}"; do
  IFS=':' read -r scenario_name duration vus <<< "$scenario_def"
  run_scenario "$scenario_name" "$duration" "$vus"

  # Cool down period between scenarios
  if [[ "$scenario_name" != "notification_processing_lag" ]]; then  # No need to cool down after last
    echo "Cooling down for 30 seconds..."
    sleep 30
    echo ""
  fi
done

echo "=== All scenarios completed ==="
echo "Finished at: $(date)"
echo ""
echo "Results saved to: $RESULTS_DIR"
echo ""
echo "To generate reports, use the PERFORMANCE_REPORT_TEMPLATE.md"
echo "and fill in the measured values from the result files."