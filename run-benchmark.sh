#!/bin/bash

# REST Performance Benchmark Runner
# Usage: ./run-benchmark.sh [variant] [scenario]
# Example: ./run-benchmark.sh a all
#          ./run-benchmark.sh c 1-read-heavy

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
VARIANT=${1:-a}
SCENARIO=${2:-all}
RESULTS_DIR="jmeter/results"
WAIT_TIME=30

# Validate variant
case $VARIANT in
  a|A)
    VARIANT_NAME="variant-a"
    PORT=8081
    PROFILE="variant-a"
    ;;
  c|C)
    VARIANT_NAME="variant-c"
    PORT=8082
    PROFILE="variant-c"
    ;;
  d|D)
    VARIANT_NAME="variant-d"
    PORT=8083
    PROFILE="variant-d"
    ;;
  *)
    echo -e "${RED}Error: Invalid variant. Use 'a', 'c', or 'd'${NC}"
    exit 1
    ;;
esac

echo -e "${GREEN}╔════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  REST Performance Benchmark Runner        ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Variant:${NC} $VARIANT_NAME (Port: $PORT)"
echo -e "${YELLOW}Scenario:${NC} $SCENARIO"
echo ""

# Create results directory
mkdir -p $RESULTS_DIR

# Function to stop variant
stop_variant() {
  echo -e "${YELLOW}Stopping $VARIANT_NAME...${NC}"
  cd docker
  docker-compose --profile $PROFILE down
  cd ..
}

# Function to start variant
start_variant() {
  echo -e "${YELLOW}Starting $VARIANT_NAME...${NC}"
  cd docker
  docker-compose --profile $PROFILE up -d
  cd ..
  
  echo -e "${YELLOW}Waiting ${WAIT_TIME}s for startup...${NC}"
  sleep $WAIT_TIME
  
  # Health check
  echo -e "${YELLOW}Checking health...${NC}"
  if curl -s http://localhost:$PORT/categories?page=0&size=1 > /dev/null; then
    echo -e "${GREEN}✓ $VARIANT_NAME is healthy${NC}"
  else
    echo -e "${RED}✗ $VARIANT_NAME health check failed${NC}"
    exit 1
  fi
}

# Function to run JMeter scenario
run_jmeter() {
  local scenario_file=$1
  local scenario_name=$(basename $scenario_file .jmx)
  local output_file="${RESULTS_DIR}/${scenario_name}-${VARIANT_NAME}.jtl"
  local report_dir="${RESULTS_DIR}/${scenario_name}-${VARIANT_NAME}-report"
  
  # Clean up existing results
  rm -f "$output_file"
  rm -rf "$report_dir"
  
  echo ""
  echo -e "${GREEN}═══════════════════════════════════════════${NC}"
  echo -e "${GREEN}Running: $scenario_name${NC}"
  echo -e "${GREEN}═══════════════════════════════════════════${NC}"
  
  jmeter -n \
    -t "jmeter/scenarios/$scenario_file" \
    -Jtarget.host=localhost \
    -Jtarget.port=$PORT \
    -l "$output_file" \
    -e -o "$report_dir"
  
  if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ $scenario_name completed successfully${NC}"
    echo -e "${YELLOW}  Results: $output_file${NC}"
    echo -e "${YELLOW}  Report:  $report_dir/index.html${NC}"
  else
    echo -e "${RED}✗ $scenario_name failed${NC}"
  fi
}

# Function to export metrics
export_metrics() {
  local metrics_file="${RESULTS_DIR}/metrics-${VARIANT_NAME}-$(date +%Y%m%d-%H%M%S).txt"
  
  echo ""
  echo -e "${YELLOW}Exporting Prometheus metrics...${NC}"
  
  if [ "$VARIANT" == "a" ]; then
    curl -s http://localhost:9091/metrics > "$metrics_file"
  else
    curl -s http://localhost:$PORT/actuator/prometheus > "$metrics_file"
  fi
  
  echo -e "${GREEN}✓ Metrics exported to: $metrics_file${NC}"
}

# Trap to ensure cleanup
trap stop_variant EXIT

# Stop any running variants
echo -e "${YELLOW}Cleaning up any running variants...${NC}"
cd docker
docker-compose --profile variant-a down 2>/dev/null || true
docker-compose --profile variant-c down 2>/dev/null || true
docker-compose --profile variant-d down 2>/dev/null || true
cd ..

# Start the variant
start_variant

# Run scenarios
if [ "$SCENARIO" == "all" ]; then
  scenarios=(
    "1-read-heavy.jmx"
    "2-join-filter.jmx"
    "3-mixed-writes.jmx"
    "4-heavy-body.jmx"
  )
  
  for scenario in "${scenarios[@]}"; do
    run_jmeter "$scenario"
    echo -e "${YELLOW}Cooldown period (60s)...${NC}"
    sleep 60
  done
else
  run_jmeter "${SCENARIO}.jmx"
fi

# Export metrics
export_metrics

# Summary
echo ""
echo -e "${GREEN}╔════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  Benchmark Completed Successfully!        ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Results location:${NC} $RESULTS_DIR/"
echo -e "${YELLOW}View reports:${NC} Open HTML files in browser"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "  1. Review JMeter HTML reports"
echo "  2. Check Grafana dashboards: http://localhost:3000"
echo "  3. Compare metrics across variants"
echo ""

# Stop variant (trap will handle this)
