# REST Web Services Performance Benchmark

Comprehensive performance benchmark comparing three REST API implementation approaches:
- **Variant A**: JAX-RS (Jersey) + JPA/Hibernate
- **Variant C**: Spring Boot + @RestController + JPA/Hibernate  
- **Variant D**: Spring Boot + Spring Data REST

## 📋 Table of Contents

- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [API Endpoints](#api-endpoints)
- [Running the Benchmarks](#running-the-benchmarks)
- [Monitoring & Metrics](#monitoring--metrics)
- [Test Scenarios](#test-scenarios)
- [Query Optimization Modes](#query-optimization-modes)
- [Results Analysis](#results-analysis)
- [Troubleshooting](#troubleshooting)

## 🎯 Project Overview

This project implements a complete performance benchmarking suite to evaluate REST API stack choices based on:

- **Latency**: p50/p95/p99 percentiles
- **Throughput**: Requests per second (RPS)
- **Error Rate**: Percentage of failed requests
- **Resource Usage**: CPU, RAM, GC activity, thread count
- **Abstraction Cost**: Manual controllers vs automatic exposure

### Business Domain

Simple e-commerce catalog with two entities:
- **Category** (1) ↔ **Item** (N)

### Test Data

- **Categories**: 2,000 records
- **Items**: 100,000 records (~50 items per category)
- **Payloads**: 0.5-1 KB (light), 5 KB (heavy)

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                       Load Testing                          │
│                    JMeter (4 Scenarios)                     │
└──────────────────┬──────────────────────────────────────────┘
                   │
    ┌──────────────┼──────────────┬──────────────────┐
    │              │              │                  │
    ▼              ▼              ▼                  ▼
┌─────────┐  ┌──────────┐  ┌──────────┐      ┌──────────┐
│Variant A│  │Variant C │  │Variant D │      │PostgreSQL│
│ Jersey  │  │Spring MVC│  │Data REST │◄─────┤  14+     │
│         │  │          │  │          │      │          │
└────┬────┘  └────┬─────┘  └────┬─────┘      └──────────┘
     │            │             │
     │            ▼             │
     │      ┌──────────┐        │
     └─────►│Prometheus│◄───────┘
            └────┬─────┘
                 │
                 ▼
            ┌─────────┐
            │ Grafana │
            └─────────┘
                 ▲
                 │
            ┌────┴─────┐
            │ InfluxDB │
            │ (JMeter) │
            └──────────┘
```

## ✅ Prerequisites

- **Java 17** (JDK)
- **Maven 3.8+**
- **Docker & Docker Compose**
- **JMeter 5.6+** (for running load tests)
- **Git**

Minimum system requirements:
- 4 CPU cores
- 8 GB RAM
- 10 GB disk space

## 🚀 Quick Start

### 1. Clone and Build

```bash
# Clone the repository
cd /home/talfaza/Desktop/uni/benchMark

# Build all variants
mvn clean install -DskipTests
```

### 2. Start Infrastructure

```bash
cd docker

# Start PostgreSQL, Prometheus, Grafana, InfluxDB
docker-compose up -d postgres prometheus grafana influxdb

# Wait for PostgreSQL to initialize (check logs)
docker-compose logs -f postgres
```

The database will be automatically populated with test data via `init.sql`.

### 3. Start a Variant

**Option A: Using Docker (Recommended)**

```bash
# Variant A (Jersey) - Port 8081
docker-compose --profile variant-a up -d

# Variant C (Spring MVC) - Port 8082
docker-compose --profile variant-c up -d

# Variant D (Spring Data REST) - Port 8083
docker-compose --profile variant-d up -d
```

**Option B: Running Locally**

```bash
# Variant A
cd variant-a-jersey
mvn spring-boot:run

# Variant C
cd variant-c-spring-mvc
mvn spring-boot:run

# Variant D
cd variant-d-spring-data-rest
mvn spring-boot:run
```

### 4. Verify API is Running

```bash
# Test Categories endpoint
curl http://localhost:8081/categories?page=0&size=5

# Test Items endpoint
curl http://localhost:8081/items?page=0&size=5

# Test relationship endpoint
curl http://localhost:8081/categories/1/items?page=0&size=5
```

### 5. Access Monitoring Dashboards

- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **InfluxDB**: http://localhost:8086 (admin/adminadmin)

### 6. Run Load Tests

```bash
cd jmeter

# Run Scenario 1: READ-heavy
jmeter -n -t scenarios/1-read-heavy.jmx \
  -Jtarget.host=localhost \
  -Jtarget.port=8081 \
  -l results/read-heavy-variant-a.jtl

# Run Scenario 2: JOIN-filter
jmeter -n -t scenarios/2-join-filter.jmx \
  -Jtarget.host=localhost \
  -Jtarget.port=8081 \
  -l results/join-filter-variant-a.jtl

# Run Scenario 3: MIXED writes
jmeter -n -t scenarios/3-mixed-writes.jmx \
  -Jtarget.host=localhost \
  -Jtarget.port=8081 \
  -l results/mixed-writes-variant-a.jtl

# Run Scenario 4: HEAVY body
jmeter -n -t scenarios/4-heavy-body.jmx \
  -Jtarget.host=localhost \
  -Jtarget.port=8081 \
  -l results/heavy-body-variant-a.jtl
```

## 📁 Project Structure

```
rest-performance-benchmark/
├── pom.xml                          # Parent POM
├── README.md                        # This file
├── .gitignore
│
├── database/
│   └── init.sql                     # Database schema + test data
│
├── variant-a-jersey/                # JAX-RS Jersey implementation
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/benchmark/jersey/
│       │   ├── entity/              # JPA entities
│       │   ├── repository/          # Data access layer
│       │   ├── resource/            # JAX-RS resources (controllers)
│       │   ├── config/              # Configuration
│       │   └── JerseyApplication.java
│       └── resources/
│           ├── META-INF/persistence.xml
│           └── application.properties
│
├── variant-c-spring-mvc/            # Spring Boot @RestController
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/benchmark/springmvc/
│       │   ├── entity/              # JPA entities
│       │   ├── repository/          # Spring Data JPA repositories
│       │   ├── controller/          # @RestController classes
│       │   └── SpringMvcApplication.java
│       └── resources/
│           └── application.yml
│
├── variant-d-spring-data-rest/      # Spring Data REST
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/benchmark/springdatarest/
│       │   ├── entity/              # JPA entities
│       │   ├── repository/          # @RepositoryRestResource
│       │   └── SpringDataRestApplication.java
│       └── resources/
│           └── application.yml
│
├── docker/
│   ├── docker-compose.yml           # Complete infrastructure
│   ├── prometheus.yml               # Prometheus scrape config
│   ├── jmx-exporter-config.yml      # JMX metrics export
│   └── grafana/
│       ├── provisioning/
│       │   ├── datasources/         # Prometheus + InfluxDB
│       │   └── dashboards/
│       └── dashboards/
│           └── jvm-dashboard.json
│
└── jmeter/
    ├── scenarios/                   # Test plans (.jmx)
    │   ├── 1-read-heavy.jmx
    │   ├── 2-join-filter.jmx
    │   ├── 3-mixed-writes.jmx
    │   └── 4-heavy-body.jmx
    ├── test-data/                   # CSV data files
    │   ├── categories.csv
    │   ├── items.csv
    │   ├── payload-1kb.json
    │   ├── payload-5kb.json
    │   └── category-payload.json
    └── results/                     # Test results (gitignored)
```

## 🗄️ Database Schema

```sql
CREATE TABLE category (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(32) UNIQUE NOT NULL,
    name        VARCHAR(128) NOT NULL,
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE item (
    id          BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(64) UNIQUE NOT NULL,
    name        VARCHAR(128) NOT NULL,
    price       NUMERIC(10,2) NOT NULL,
    stock       INT NOT NULL,
    category_id BIGINT NOT NULL REFERENCES category(id),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_item_category ON item(category_id);
CREATE INDEX idx_item_updated_at ON item(updated_at);
```

**Relationship**: `Category (1) ↔ Item (N)`

**JPA Mapping**:
- `Item.category`: `@ManyToOne(fetch = LAZY)`
- `Category.items`: `@OneToMany(mappedBy = "category")`

## 🔌 API Endpoints

All variants implement the same functional endpoints:

### Category Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/categories?page=0&size=20` | Paginated list |
| GET | `/categories/{id}` | Get by ID |
| POST | `/categories` | Create |
| PUT | `/categories/{id}` | Update |
| DELETE | `/categories/{id}` | Delete |
| GET | `/categories/{id}/items?page=0&size=20` | Items in category |

### Item Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/items?page=0&size=20` | Paginated list |
| GET | `/items?categoryId={id}&page=0&size=20` | Filter by category |
| GET | `/items/{id}` | Get by ID |
| POST | `/items` | Create |
| PUT | `/items/{id}` | Update |
| DELETE | `/items/{id}` | Delete |

### Variant-Specific Notes

**Variant D (Spring Data REST)**:
- Uses HAL (Hypertext Application Language) format by default
- Exposes additional HATEOAS links
- Auto-generated endpoints from repositories
- Access via: `/categories`, `/items`, etc.

**Example Request**:

```bash
# Create Category
curl -X POST http://localhost:8081/categories \
  -H "Content-Type: application/json" \
  -d '{"code":"CAT9999","name":"Test Category"}'

# Create Item
curl -X POST http://localhost:8081/items \
  -H "Content-Type: application/json" \
  -d '{
    "sku":"SKU123456",
    "name":"Test Item",
    "price":99.99,
    "stock":100,
    "category":{"id":1}
  }'
```

## 🧪 Running the Benchmarks

### Complete Benchmark Workflow

**1. Prepare Environment**

```bash
# Stop all running variants
docker-compose --profile variant-a down
docker-compose --profile variant-c down
docker-compose --profile variant-d down

# Clear database (optional - for fresh start)
docker-compose down -v postgres
docker-compose up -d postgres

# Wait for DB initialization
sleep 30
```

**2. Test Variant A (Jersey)**

```bash
# Start Variant A
docker-compose --profile variant-a up -d
sleep 30  # Wait for startup

# Run all scenarios
cd jmeter
for scenario in 1-read-heavy 2-join-filter 3-mixed-writes 4-heavy-body; do
  echo "Running $scenario on Variant A..."
  jmeter -n -t scenarios/${scenario}.jmx \
    -Jtarget.host=localhost \
    -Jtarget.port=8081 \
    -l results/${scenario}-variant-a.jtl \
    -e -o results/${scenario}-variant-a-report
done

# Export Prometheus metrics
curl http://localhost:9091/metrics > results/metrics-variant-a.txt

# Stop Variant A
docker-compose --profile variant-a down
```

**3. Test Variant C (Spring MVC)**

```bash
# Start Variant C
docker-compose --profile variant-c up -d
sleep 30

# Run all scenarios
for scenario in 1-read-heavy 2-join-filter 3-mixed-writes 4-heavy-body; do
  echo "Running $scenario on Variant C..."
  jmeter -n -t scenarios/${scenario}.jmx \
    -Jtarget.host=localhost \
    -Jtarget.port=8082 \
    -l results/${scenario}-variant-c.jtl \
    -e -o results/${scenario}-variant-c-report
done

# Export metrics
curl http://localhost:8082/actuator/prometheus > results/metrics-variant-c.txt

# Stop Variant C
docker-compose --profile variant-c down
```

**4. Test Variant D (Spring Data REST)**

```bash
# Start Variant D
docker-compose --profile variant-d up -d
sleep 30

# Run all scenarios
for scenario in 1-read-heavy 2-join-filter 3-mixed-writes 4-heavy-body; do
  echo "Running $scenario on Variant D..."
  jmeter -n -t scenarios/${scenario}.jmx \
    -Jtarget.host=localhost \
    -Jtarget.port=8083 \
    -l results/${scenario}-variant-d.jtl \
    -e -o results/${scenario}-variant-d-report
done

# Export metrics
curl http://localhost:8083/actuator/prometheus > results/metrics-variant-d.txt

# Stop Variant D
docker-compose --profile variant-d down
```

## 📊 Monitoring & Metrics

### Prometheus Metrics

All variants expose metrics for monitoring:

**Variant A (Jersey)**: `http://localhost:9091/metrics` (JMX Exporter)
**Variant C & D (Spring)**: `http://localhost:808X/actuator/prometheus`

Key metrics:
- `jvm_memory_heap_used` / `jvm_memory_heap_max`
- `jvm_gc_collection_time_ms`
- `jvm_threads_current`
- `jvm_process_cpu_load`
- `hikari_active_connections` / `hikari_total_connections`

### Grafana Dashboards

Access: http://localhost:3000 (admin/admin)

Pre-configured dashboards show:
- Heap memory usage
- GC activity
- Thread count
- CPU usage
- HikariCP connection pool metrics

### InfluxDB (JMeter Metrics)

JMeter sends real-time metrics to InfluxDB:
- Request count
- Response times (p50, p95, p99)
- Throughput (RPS)
- Error rate

Access InfluxDB UI: http://localhost:8086

## 🎯 Test Scenarios

### Scenario 1: READ-Heavy (Relation Included)

**Mix**:
- 50% GET `/items?page=&size=50`
- 20% GET `/items?categoryId=...`
- 20% GET `/categories/{id}/items`
- 10% GET `/categories?page=&size=`

**Load**: 50 → 100 → 200 threads (3 paliers)
**Duration**: 10 min/palier, 60s ramp-up
**File**: `1-read-heavy.jmx`

### Scenario 2: JOIN-Filter Targeted

**Mix**:
- 70% GET `/items?categoryId=...`
- 30% GET `/items/{id}`

**Load**: 60 → 120 threads (2 paliers)
**Duration**: 8 min/palier, 60s ramp-up
**File**: `2-join-filter.jmx`

### Scenario 3: MIXED (Writes on Two Entities)

**Mix**:
- 40% GET `/items`
- 20% POST `/items` (1 KB)
- 10% PUT `/items/{id}` (1 KB)
- 10% DELETE `/items/{id}`
- 10% POST `/categories` (0.5 KB)
- 10% PUT `/categories/{id}`

**Load**: 50 → 100 threads (2 paliers)
**Duration**: 10 min/palier, 60s ramp-up
**File**: `3-mixed-writes.jmx`

### Scenario 4: HEAVY-Body (5 KB Payload)

**Mix**:
- 50% POST `/items` (5 KB)
- 50% PUT `/items/{id}` (5 KB)

**Load**: 30 → 60 threads (2 paliers)
**Duration**: 8 min/palier, 60s ramp-up
**File**: `4-heavy-body.jmx`

## ⚙️ Query Optimization Modes

Each variant supports two query modes to measure N+1 query impact:

### Baseline Mode (N+1 Queries)

```java
// Triggers N+1 selects
Page<Item> items = repository.findAll(pageable);
// Accessing item.getCategory() causes additional SELECT per item
```

### Optimized Mode (JOIN FETCH)

```java
// Single query with join
@Query("SELECT i FROM Item i JOIN FETCH i.category")
Page<Item> items = repository.findAllOptimized(pageable);
```

**Configure Mode**:

Via environment variable:
```bash
# Optimized (default)
export QUERY_MODE=optimized

# Baseline (to measure N+1 impact)
export QUERY_MODE=baseline
```

Or in Docker Compose:
```yaml
environment:
  QUERY_MODE: optimized  # or baseline
```

## 📈 Results Analysis

### JMeter Results

After running tests, JMeter generates:

**JTL Files** (raw results):
```bash
jmeter/results/
├── 1-read-heavy-variant-a.jtl
├── 2-join-filter-variant-a.jtl
└── ...
```

**HTML Reports**:
```bash
jmeter/results/
├── 1-read-heavy-variant-a-report/
│   └── index.html  # Open in browser
```

**Key Metrics to Extract**:

From HTML reports:
- **Throughput**: Transactions/second
- **Response Time**: p50, p95, p99 percentiles
- **Error %**: Failed requests percentage

From Prometheus exports:
- **CPU**: `jvm_process_cpu_load`
- **Memory**: `jvm_memory_heap_used`
- **GC**: `jvm_gc_collection_time_ms`
- **Threads**: `jvm_threads_current`

### Comparison Tables

Fill in the provided tables (T0-T7) from the assignment document:

**T2 - JMeter Results Example**:

| Scenario | Measure | A: Jersey | C: @RestController | D: Spring Data REST |
|----------|---------|-----------|-------------------|---------------------|
| READ-heavy | RPS | 1250 | 1320 | 1180 |
| READ-heavy | p50 (ms) | 35 | 32 | 38 |
| READ-heavy | p95 (ms) | 125 | 110 | 145 |
| READ-heavy | p99 (ms) | 250 | 220 | 280 |
| READ-heavy | Err % | 0.1 | 0.05 | 0.2 |

**T3 - JVM Resources Example**:

| Variant | CPU (%) | Heap (MB) | GC (ms/s) | Threads |
|---------|---------|-----------|-----------|---------|
| A: Jersey | 45/80 | 450/750 | 5/15 | 85/120 |
| C: @RestController | 42/75 | 480/800 | 4/12 | 90/125 |
| D: Spring Data REST | 48/85 | 520/820 | 6/18 | 95/130 |

## 🔧 Troubleshooting

### Common Issues

**1. Port Already in Use**

```bash
# Check what's using the port
sudo lsof -i :8081

# Kill the process
kill -9 <PID>
```

**2. Database Connection Refused**

```bash
# Check PostgreSQL is running
docker-compose ps postgres

# View logs
docker-compose logs postgres

# Restart database
docker-compose restart postgres
```

**3. Out of Memory**

Increase JVM heap:
```bash
# Edit docker-compose.yml
environment:
  JAVA_OPTS: "-Xms1g -Xmx2g -XX:+UseG1GC"
```

**4. JMeter Backend Listener Errors**

Verify InfluxDB is running:
```bash
docker-compose ps influxdb
curl http://localhost:8086/health
```

**5. Slow Queries (N+1)**

Check query mode:
```bash
# Verify environment variable
docker-compose exec variant-c-spring-mvc env | grep QUERY_MODE

# Should be "optimized" for best performance
```

### Viewing Logs

```bash
# Variant A logs
docker-compose logs -f variant-a-jersey

# Variant C logs
docker-compose logs -f variant-c-spring-mvc

# Variant D logs
docker-compose logs -f variant-d-spring-data-rest

# All services
docker-compose logs -f
```

## 🎓 Best Practices

### For Accurate Benchmarks

1. **Isolate Tests**: Run only one variant at a time
2. **Warm-up**: Discard first 2 minutes of each test
3. **Stable Environment**: Close other applications
4. **Multiple Runs**: Run each scenario 3 times, average results
5. **Cool Down**: Wait 2-3 minutes between tests
6. **Same Configuration**: Use identical HikariCP, JVM settings

### HikariCP Configuration

All variants use identical connection pool settings:
```yaml
minimum-idle: 10
maximum-pool-size: 20
idle-timeout: 300000
connection-timeout: 20000
max-lifetime: 1200000
```

### JVM Configuration

All variants use identical JVM flags:
```bash
-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

## 📚 References

### Technologies Used

- **Java 17**: LTS version with modern features
- **Spring Boot 3.1.5**: Latest stable release
- **Hibernate 6.2**: JPA implementation
- **Jersey 3.1**: JAX-RS reference implementation
- **PostgreSQL 14**: Relational database
- **HikariCP 5.0**: JDBC connection pool
- **JMeter 5.6**: Load testing tool
- **Prometheus**: Metrics collection
- **Grafana**: Visualization
- **InfluxDB 2.7**: Time-series database

### Documentation

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data REST Guide](https://spring.io/guides/gs/accessing-data-rest/)
- [Jersey Documentation](https://eclipse-ee4j.github.io/jersey/)
- [JMeter User Manual](https://jmeter.apache.org/usermanual/)
- [Prometheus Documentation](https://prometheus.io/docs/)

## 📝 License

This project is created for educational purposes as part of a university performance benchmarking assignment.

## 👥 Authors

[Your Names Here]

---

**Date**: November 2025  
**Course**: [Course Name]  
**Professor**: Pr. LACHGAR
