#!/bin/bash

# Initial setup script for REST Performance Benchmark
# Run this once before starting benchmarks

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}╔════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  REST Performance Benchmark Setup         ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════╝${NC}"
echo ""

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

command -v java >/dev/null 2>&1 || { echo -e "${RED}✗ Java not found${NC}"; exit 1; }
echo -e "${GREEN}✓ Java$(java -version 2>&1 | head -1)${NC}"

command -v mvn >/dev/null 2>&1 || { echo -e "${RED}✗ Maven not found${NC}"; exit 1; }
echo -e "${GREEN}✓ Maven $(mvn -version | head -1)${NC}"

command -v docker >/dev/null 2>&1 || { echo -e "${RED}✗ Docker not found${NC}"; exit 1; }
echo -e "${GREEN}✓ Docker $(docker --version)${NC}"

command -v docker-compose >/dev/null 2>&1 || { echo -e "${RED}✗ Docker Compose not found${NC}"; exit 1; }
echo -e "${GREEN}✓ Docker Compose $(docker-compose --version)${NC}"

if command -v jmeter >/dev/null 2>&1; then
  echo -e "${GREEN}✓ JMeter $(jmeter --version 2>&1 | head -1)${NC}"
else
  echo -e "${YELLOW}⚠ JMeter not found in PATH${NC}"
  echo -e "${YELLOW}  Please install JMeter or add it to PATH${NC}"
fi

echo ""

# Build applications
echo -e "${YELLOW}Building applications...${NC}"
mvn clean install -DskipTests

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ Build successful${NC}"
else
  echo -e "${RED}✗ Build failed${NC}"
  exit 1
fi

echo ""

# Start infrastructure
echo -e "${YELLOW}Starting infrastructure (PostgreSQL, Prometheus, Grafana, InfluxDB)...${NC}"
cd docker
docker-compose up -d postgres prometheus grafana influxdb
cd ..

echo -e "${YELLOW}Waiting for services to start (45s)...${NC}"
sleep 45

# Check services
echo ""
echo -e "${YELLOW}Checking service health...${NC}"

if curl -s http://localhost:9090/-/healthy > /dev/null; then
  echo -e "${GREEN}✓ Prometheus is healthy${NC}"
else
  echo -e "${RED}✗ Prometheus is not responding${NC}"
fi

if curl -s http://localhost:3000/api/health > /dev/null; then
  echo -e "${GREEN}✓ Grafana is healthy${NC}"
else
  echo -e "${YELLOW}⚠ Grafana might still be starting${NC}"
fi

if curl -s http://localhost:8086/health > /dev/null; then
  echo -e "${GREEN}✓ InfluxDB is healthy${NC}"
else
  echo -e "${RED}✗ InfluxDB is not responding${NC}"
fi

if docker-compose -f docker/docker-compose.yml exec -T postgres pg_isready -U benchmark > /dev/null 2>&1; then
  echo -e "${GREEN}✓ PostgreSQL is healthy${NC}"
else
  echo -e "${RED}✗ PostgreSQL is not responding${NC}"
fi

echo ""

# Verify database
echo -e "${YELLOW}Verifying database initialization...${NC}"
CATEGORY_COUNT=$(docker-compose -f docker/docker-compose.yml exec -T postgres \
  psql -U benchmark -d benchmark -t -c "SELECT COUNT(*) FROM category;" 2>/dev/null | tr -d ' ')

ITEM_COUNT=$(docker-compose -f docker/docker-compose.yml exec -T postgres \
  psql -U benchmark -d benchmark -t -c "SELECT COUNT(*) FROM item;" 2>/dev/null | tr -d ' ')

if [ "$CATEGORY_COUNT" -ge 2000 ] && [ "$ITEM_COUNT" -ge 100000 ]; then
  echo -e "${GREEN}✓ Database initialized: $CATEGORY_COUNT categories, $ITEM_COUNT items${NC}"
else
  echo -e "${YELLOW}⚠ Database might still be initializing${NC}"
  echo -e "${YELLOW}  Categories: $CATEGORY_COUNT / 2000${NC}"
  echo -e "${YELLOW}  Items: $ITEM_COUNT / 100000${NC}"
fi

echo ""

# Create results directory
mkdir -p jmeter/results
echo -e "${GREEN}✓ Created results directory${NC}"

# Make benchmark script executable
chmod +x run-benchmark.sh
echo -e "${GREEN}✓ Made benchmark script executable${NC}"

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  Setup Complete!                          ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Access Points:${NC}"
echo "  • Grafana:    http://localhost:3000 (admin/admin)"
echo "  • Prometheus: http://localhost:9090"
echo "  • InfluxDB:   http://localhost:8086 (admin/adminadmin)"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "  1. Run benchmarks with: ./run-benchmark.sh [variant] [scenario]"
echo "     Examples:"
echo "       ./run-benchmark.sh a all       # All scenarios on Variant A"
echo "       ./run-benchmark.sh c 1-read-heavy  # Single scenario on Variant C"
echo ""
echo "  2. View monitoring dashboards in Grafana"
echo ""
echo "  3. Analyze results in jmeter/results/"
echo ""
