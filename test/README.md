# CooBS Core Retail Banking — JMeter Performance & Load Test Suite

This directory contains the production-grade **Apache JMeter Load Testing Suite** for the **CooBS Core Retail Banking Platform**.

---

## 1. Overview & Test Architecture

The load test plan (`jmeter-load-test.jmx`) simulates realistic, high-volume retail banking operations targeting the **Spring Cloud API Gateway (`:8080`)**.

```
                           ┌─────────────────────────────────────────┐
                           │      Apache JMeter Test Engine          │
                           │  (CLI Headless or Interactive GUI Mode) │
                           └──────────────────┬──────────────────────┘
                                              │
                      ┌───────────────────────┴───────────────────────┐
                      │                                               │
             [setUp / OnceOnly]                             [Main Transaction Loop]
      Authenticate Virtual Users                      Continuous Retail Banking Mix
       - john_doe (Customer 1)                                        │
       - maria_santos (Customer 2)                   ┌────────────────┴────────────────┐
       - Dynamic JWT Extraction                      │                                 │
                                            40% Portfolio Query               10% Ping Transfer (1 -> 2)
                                            30% Balance Inquiries             10% Pong Transfer (2 -> 1)
                                            10% Notification Feed
                                                     │
                                                     ▼
                                      ┌─────────────────────────────┐
                                      │  Spring Cloud API Gateway   │
                                      │         (:8080)             │
                                      └──────────────┬──────────────┘
                                                     │
                             ┌───────────────────────┼───────────────────────┐
                             ▼                       ▼                       ▼
                     account-service          ledger-service         notification-service
                         (:8082)                 (:8083)                   (:8084)
```

---

## 2. Key Safeguards & Features

- **Dynamic JWT Authentication (`OnceOnlyController`)**:
  - Automatically invokes `POST /api/v1/auth/customers/login` at the start of each virtual user session.
  - Extracts Bearer access tokens dynamically using JSON Path extractors (`$.authData.accessToken`).
  - No expired or hardcoded tokens; avoids triggering edge authentication rate limits.

- **Safe Closed-Loop "Ping-Pong" Balance Equilibrium**:
  - Transacting accounts: **Account 1 (`john_doe`)** and **Account 2 (`maria_santos`)**.
  - Symmetric 10% Ping (`Account 1 -> Account 2`) and 10% Pong (`Account 2 -> Account 1`) transfers.
  - Nominal transfer amounts ($1.00 PHP) prevent account balance exhaustion even during prolonged stress runs.

- **Rate-Limiter Aligned Pacing**:
  - Incorporates Gaussian Random Timer think-time (200ms–400ms delay) per thread.
  - Smooths request bursts to respect Redis rate limiting policies on the API Gateway.

- **Strict Contract & SLA Assertions**:
  - Status code verification: HTTP `200 OK` for queries, HTTP `201 Created` for fund transfers.
  - Duration SLA Assertion: Flags any transaction taking $> 3000\text{ ms}$.

---

## 3. Quick Start & Execution

### Prerequisites
1. Ensure the platform backend microservices and databases are running:
   ```bash
   docker compose --profile app up -d
   ```
2. Verify Apache JMeter (v5.6+) is installed and available in your `PATH`:
   ```bash
   jmeter -v
   ```

---

### Method A: Automated PowerShell Runner (Recommended)

Use the built-in runner script [`scripts/run_jmeter_test.ps1`](../scripts/run_jmeter_test.ps1):

```powershell
# Run default test (5 virtual users, 10s ramp-up, 60s duration):
.\scripts\run_jmeter_test.ps1

# Run with custom concurrency and auto-open HTML Dashboard report:
.\scripts\run_jmeter_test.ps1 -Threads 10 -Duration 120 -Report

# Open test plan directly in the JMeter GUI:
.\scripts\run_jmeter_test.ps1 -Gui
```

#### Script Parameters:
| Parameter | Default | Description |
| :--- | :---: | :--- |
| `-HostName` | `localhost` | Gateway hostname |
| `-Port` | `8080` | Gateway port |
| `-Threads` | `5` | Number of concurrent virtual user threads |
| `-RampUp` | `10` | Thread ramp-up time in seconds |
| `-Duration` | `60` | Total test duration in seconds |
| `-Amount` | `1.00` | Nominal transfer amount in PHP |
| `-SlaMs` | `3000` | SLA response time threshold in milliseconds |
| `-DelayMs` | `300` | Pacing think-time between requests in milliseconds |
| `-Clean` | `$true` | Cleans previous test results and reports |
| `-Report` | _(switch)_ | Automatically opens HTML report in default browser when done |
| `-Gui` | _(switch)_ | Opens interactive JMeter GUI |

---

### Method B: Native JMeter CLI (Non-GUI CI/CD Mode)

Execute directly via Apache JMeter command line:

```bash
jmeter -n -t test/jmeter-load-test.jmx \
  -l test/results/results.jtl \
  -e -o test/results/html-report \
  -JHOST=localhost \
  -JPORT=8080 \
  -JTHREADS=5 \
  -JRAMP_UP=10 \
  -JDURATION=60 \
  -JTRANSFER_AMOUNT=1.00 \
  -JSLA_MS=3000 \
  -JDELAY_MS=300
```

---

## 4. Analyzing Test Results & Reports

Upon test completion, an interactive HTML dashboard is generated at:
`test/results/html-report/index.html`

The dashboard provides:
1. **APDEX (Application Performance Index)**: Performance rating according to SLA tolerances.
2. **Throughput & RPS**: Real-time transactions per second across endpoints.
3. **Response Time Percentiles**: $p_{50}$, $p_{90}$, $p_{95}$, and $p_{99}$ latency distributions.
4. **Error Distribution**: Verification that error rate is 0.00%.

While running load tests, observe live database locks, JVM heap, HikariCP pool utilization, and distributed traces in Grafana:
- **Grafana Operational Dashboard**: `http://localhost:3001`
- **Prometheus Metrics**: `http://localhost:9090`
- **Jaeger Distributed Tracing**: `http://localhost:16686`
- **Kafka Topic Event Flow**: `http://localhost:8088`
