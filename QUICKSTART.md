# Quick Start Guide

## ⚡ 5-Minute Setup

### 1. Run Setup Script

```bash
chmod +x setup.sh
./setup.sh
```

This will:
- Build all 3 variants
- Start PostgreSQL with test data
- Start Prometheus, Grafana, InfluxDB
- Verify everything is working

### 2. Run Your First Benchmark

```bash
# Test Variant A (Jersey) with all scenarios
./run-benchmark.sh a all
```

### 3. View Results

**JMeter Reports**:
```bash
# Open HTML report in browser
xdg-open jmeter/results/1-read-heavy-variant-a-report/index.html
```

**Grafana Dashboards**:
```bash
# Open in browser
xdg-open http://localhost:3000
# Login: admin/admin
```

## 📊 Running Other Variants

```bash
# Variant C (Spring MVC) - all scenarios
./run-benchmark.sh c all

# Variant D (Spring Data REST) - single scenario
./run-benchmark.sh d 1-read-heavy
```

## 🎯 Test Scenarios Available

1. **1-read-heavy**: Mixed read operations with relations
2. **2-join-filter**: Focused JOIN and filter queries
3. **3-mixed-writes**: CRUD operations on both entities
4. **4-heavy-body**: Large payload (5KB) POST/PUT

## 🔍 Manual Testing

### Start a Variant

```bash
cd docker

# Variant A (Port 8081)
docker-compose --profile variant-a up -d

# Variant C (Port 8082)
docker-compose --profile variant-c up -d

# Variant D (Port 8083)
docker-compose --profile variant-d up -d
```

### Test Endpoints

```bash
# Get categories
curl http://localhost:8081/categories?page=0&size=5

# Get items
curl http://localhost:8081/items?page=0&size=5

# Get items by category
curl http://localhost:8081/items?categoryId=1&page=0&size=5

# Get category items
curl http://localhost:8081/categories/1/items?page=0&size=5

# Create item
curl -X POST http://localhost:8081/items \
  -H "Content-Type: application/json" \
  -d '{
    "sku":"TEST123",
    "name":"Test Item",
    "price":99.99,
    "stock":100,
    "category":{"id":1}
  }'
```

### Run Single JMeter Test

```bash
cd jmeter

jmeter -n \
  -t scenarios/1-read-heavy.jmx \
  -Jtarget.host=localhost \
  -Jtarget.port=8081 \
  -l results/test.jtl
```

## 🛑 Stopping Everything

```bash
cd docker

# Stop specific variant
docker-compose --profile variant-a down

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

## 📈 Monitoring URLs

- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **InfluxDB**: http://localhost:8086 (admin/adminadmin)
- **Variant A Metrics**: http://localhost:9091/metrics
- **Variant C Actuator**: http://localhost:8082/actuator
- **Variant D Actuator**: http://localhost:8083/actuator

## 🔧 Query Mode Configuration

Test N+1 query impact by switching modes:

**Edit `docker/docker-compose.yml`**:

```yaml
environment:
  QUERY_MODE: baseline   # N+1 queries
  # or
  QUERY_MODE: optimized  # JOIN FETCH
```

Then restart the variant.

## 📝 Collecting Results

Results are saved in `jmeter/results/`:

```
jmeter/results/
├── 1-read-heavy-variant-a.jtl          # Raw results
├── 1-read-heavy-variant-a-report/      # HTML report
│   └── index.html
├── metrics-variant-a-TIMESTAMP.txt     # Prometheus metrics
└── ...
```

## ❓ Troubleshooting

**Port already in use**:
```bash
sudo lsof -i :8081
kill -9 <PID>
```

**Database not initialized**:
```bash
docker-compose logs postgres
# Wait for initialization to complete
```

**Service not responding**:
```bash
docker-compose restart <service-name>
docker-compose logs -f <service-name>
```

## 📚 Full Documentation

See [README.md](README.md) for complete documentation.
