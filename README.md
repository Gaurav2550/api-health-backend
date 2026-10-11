# API Health, Observability & Latency Optimization Platform

[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()
[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)]()
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)]()

Production-grade Spring Boot 3 backend application designed for continuous API health monitoring, distributed latency observability, nearest-rank percentile telemetry (P50, P95, P99), rule-based bottleneck optimization recommendations, tiered caching (in-memory + Redis fallback), and automated incident alerting.

---

## 🏗 Architecture & Design Highlights

```
                          ┌───────────────────────────┐
                          │   Client / Frontend SPA   │
                          └─────────────┬─────────────┘
                                        │ HTTPS / JWT
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       API Health Platform Backend                           │
│                                                                             │
│  ┌───────────────────┐  ┌────────────────────┐  ┌────────────────────────┐  │
│  │   Auth & Security │  │  Monitored API     │  │  Metrics & Telemetry   │  │
│  │   (JWT, BCrypt)   │  │  Management        │  │  (P50/P95/P99, Trends) │  │
│  └───────────────────┘  └──────────┬─────────┘  └────────────────────────┘  │
│                                    │                                        │
│  ┌──────────────────────────────┐  │            ┌────────────────────────┐  │
│  │ Optimization Engine          │  │            │ Alert Lifecycle Engine │  │
│  │ (Rule-based Recommendations) │  │            │ (Trigger & Auto-Heal)  │  │
│  └──────────────────────────────┘  │            └────────────────────────┘  │
│                                    ▼                                        │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ Scheduled Health Check Engine (ThreadPoolExecutor, CallerRunsPolicy) │  │
│  │ └── SsrfValidator (Private IP, Link-Local, Cloud Metadata Blocking)   │  │
│  │ └── Non-blocking java.net.http.HttpClient                              │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│  ┌─────────────────────────────────┴─────────────────────────────────────┐  │
│  │ Tiered Caching Layer                                                  │  │
│  │ ├── L1: CustomInMemoryCache<K, V> (TTL + LRU eviction, Hit Rate stats)│  │
│  │ └── L2: Redis (Graceful fallback if Redis is unavailable)             │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
               ┌─────────────────────┴─────────────────────┐
               ▼                                           ▼
      ┌─────────────────┐                         ┌─────────────────┐
      │   PostgreSQL    │                         │      Redis      │
      │ (Flyway Schema) │                         │  (Cache Store)  │
      └─────────────────┘                         └─────────────────┘
```

### 1. Robust SSRF Protection (`SsrfValidator`)
Guards outbound health checks against Server-Side Request Forgery attacks:
- Enforces HTTP/HTTPS protocol schemes.
- Blocks loopback and localhost addresses (`127.0.0.1`, `localhost`, `[::1]`, `0.0.0.0`).
- Blocks private IPv4 subnets (RFC 1918: `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`).
- Blocks link-local and cloud metadata addresses (`169.254.169.254`).
- Blocks unauthorized ports (SSH, SMTP, MySQL, Postgres, Redis, etc.).

### 2. High-Throughput Monitoring Engine
- Dedicated `ThreadPoolExecutor` with bounded queue capacity and `CallerRunsPolicy` backpressure.
- Overlap prevention using thread-safe in-progress set (`ConcurrentHashMap.newKeySet()`).
- Bounded response body reading to protect JVM heap against payload bombs.

### 3. Nearest-Rank Percentiles & Latency Observability
- Mathematical calculation of **P50**, **P95**, and **P99** response times on raw sorted samples.
- Availability (`(UP + SLOW) / TOTAL * 100`) and error rate tracking.
- Hourly trend aggregation grouped by UTC timestamp.

### 4. Rule-Based Optimization Engine
Analyzes health check records to diagnose bottlenecks and provide actionable remediations:
- **High Error Rate / Downtime:** Suggests Circuit Breaker (Resilience4j), retry backoffs, and on-call alerting.
- **High Average Latency:** Recommends Redis caching, query indexing, and read replicas.
- **Timeouts:** Diagnoses connection pool starvation, proxy keep-alive, and gateway timeouts.
- **Tail Latency Spikes (P95 vs P50):** Suggests rate limiting, asynchronous queues (Kafka/RabbitMQ), and GC tuning.
- **Response Payloads:** Recommends Gzip/Brotli compression, pagination, and sparse fieldsets.

### 5. Tiered In-Memory + Redis Caching
- **L1 In-Memory Cache:** Generic, thread-safe cache with TTL expiration, LRU eviction upon capacity breach, and atomic performance telemetry (hits, misses, hit-rate %).
- **L2 Redis:** Distributed cache with automated failover: if Redis is unreachable, the system transparently logs and operates using in-memory cache without customer-facing errors.
- **Benchmarking Endpoint:** Built-in micro-benchmark endpoint comparing uncached computation vs L1 memory cache vs L2 Redis.

### 6. Automated Alerting & Auto-Healing
- Automatically triggers alerts on consecutive failure threshold breaches, high latency, or timeouts.
- Automatically resolves open alerts when the monitored endpoint recovers to `UP`.
- Full alert lifecycle support (Active $\to$ Acknowledged $\to$ Resolved).

---

## 🛠 Technology Stack

- **Runtime:** Java 21 LTS
- **Framework:** Spring Boot 3.3.5
- **Data Persistence:** Spring Data JPA, Hibernate ORM 6.5
- **Database:** PostgreSQL 16 (H2 Database for integration testing)
- **Database Migrations:** Flyway 10
- **Distributed Cache:** Redis 7 with Lettuce driver
- **Security:** Spring Security 6, JJWT 0.12.6 (Stateless HMAC-SHA512)
- **HTTP Client:** Modern `java.net.http.HttpClient`
- **Telemetry & Metrics:** Spring Boot Actuator, Micrometer Prometheus
- **Documentation:** SpringDoc OpenAPI 2.6.0 / Swagger UI
- **Containerization:** Docker multi-stage build, Docker Compose

---

## 🚀 Getting Started

### Prerequisites
- Java 21 JDK installed (`java -version`)
- Apache Maven 3.9+ (`mvn -version`)
- Docker & Docker Compose (optional, for full-stack deployment)

---

### Method 1: Run with Docker Compose (Recommended)

1. Start all services (PostgreSQL, Redis, Spring Boot Backend, Prometheus):
   ```bash
   docker-compose up --build -d
   ```

2. Verify services:
   ```bash
   docker-compose ps
   ```

3. Access the services:
   - **Backend API:** `http://localhost:8080`
   - **Swagger UI:** `http://localhost:8080/swagger-ui.html`
   - **OpenAPI JSON Spec:** `http://localhost:8080/api-docs`
   - **Actuator Health:** `http://localhost:8080/actuator/health`
   - **Prometheus Metrics:** `http://localhost:8080/actuator/prometheus`
   - **Prometheus UI:** `http://localhost:9090`

---

### Method 2: Local Development

1. Ensure a PostgreSQL instance is running on `localhost:5432` with database `apihealth`.
2. Run database migrations and compile:
   ```bash
   mvn clean compile
   ```
3. Run the complete test suite:
   ```bash
   mvn test
   ```
4. Start the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```

---

## 📖 API Endpoints Reference

### 🔐 Authentication (`/api/v1/auth`)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register a new user |
| `POST` | `/api/v1/auth/login` | Login and obtain JWT token |
| `GET` | `/api/v1/auth/me` | Get authenticated user profile |
| `POST` | `/api/v1/auth/logout` | Client-side token invalidation |

### 📡 Monitored APIs (`/api/v1/apis`)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/apis` | Register a new API for monitoring |
| `GET` | `/api/v1/apis` | List monitored APIs with pagination |
| `GET` | `/api/v1/apis/{id}` | Get monitored API configuration |
| `PUT` | `/api/v1/apis/{id}` | Update API monitoring parameters |
| `DELETE` | `/api/v1/apis/{id}` | Delete monitored API |
| `POST` | `/api/v1/apis/{id}/activate` | Enable scheduled checks |
| `POST` | `/api/v1/apis/{id}/deactivate` | Pause scheduled checks |
| `POST` | `/api/v1/apis/{id}/check` | Trigger manual immediate health check |
| `GET` | `/api/v1/apis/{id}/checks` | Get historical health check records |
| `GET` | `/api/v1/apis/{id}/recommendations`| Get performance optimization suggestions |

### 📊 Observability & Metrics (`/api/v1/metrics`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/metrics/dashboard` | Get high-level dashboard summary |
| `GET` | `/api/v1/metrics/apis/{id}` | Detailed metrics (P50/P95/P99, SLA) |
| `GET` | `/api/v1/metrics/apis/{id}/trends` | Hourly latency trend points |
| `GET` | `/api/v1/metrics/apis/summaries` | Summary status for all owned APIs |
| `GET` | `/api/v1/metrics/apis/slowest` | Top slowest APIs ranked by latency |

### 🚨 Alert Management (`/api/v1/alerts`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/alerts` | List alerts (filterable by status/severity) |
| `POST` | `/api/v1/alerts/{id}/acknowledge` | Acknowledge active alert |
| `POST` | `/api/v1/alerts/{id}/resolve` | Manually resolve alert |

### ⚡ Cache & Performance (`/api/v1/cache`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/cache/stats` | View L1 & L2 cache statistics and hit-rate |
| `POST` | `/api/v1/cache/clear` | Evict all cached items |
| `GET` | `/api/v1/cache/benchmark` | Run latency benchmark (Uncached vs L1 vs L2) |

---

## 🧪 Testing & Verification

The project includes unit and integration tests across all layers:
- `CustomInMemoryCacheTest`: Validates thread-safe put/get, TTL expiration, LRU eviction, and hit-rate calculation.
- `SsrfValidatorTest`: Tests rejection of loopback, private IP ranges (RFC 1918), AWS/cloud metadata, non-HTTP protocols, and restricted infrastructure ports.
- `MetricsServiceTest`: Validates the nearest-rank percentile algorithm for P50, P95, and P99 calculations.
- `OptimizationServiceTest`: Validates rule evaluation for reliability, latency breaches, and tail latency jitter.
- `AlertServiceTest`: Tests consecutive failure alert creation and automatic resolution upon health recovery.
- `AuthControllerIntegrationTest`: Tests registration, JWT token generation, duplicate email rejection (409 Conflict), and credential validation.
- `ApiPlatformIntegrationTest`: Tests end-to-end user workflows, API creation, metric retrieval, and tenant ownership isolation (403 Forbidden).

Run tests:
```bash
mvn test
```

---

## 📦 Postman Collection

Import `api-health-platform.postman_collection.json` into Postman.
The collection includes pre-configured environment variables and test scripts that automatically capture and inject the JWT Bearer token across subsequent requests.
