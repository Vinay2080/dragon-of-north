# Production-Safe k6 Load Testing Suite

Backend: `https://dragon-api.duckdns.org`

All tests are configured with:

- **Max 50 VUs** (conservative, production-safe)
- **Gradual ramping** (no sudden traffic spikes)
- **Under 2 minutes** per test
- **Built-in thresholds** for pass/fail criteria

---

## Run Commands

### 1. Health Endpoint Stability Test

```bash
cd "C:\Users\shaki\IdeaProjects\dragon-of-north\load_test"
k6 run health-stability-test.js
```

### 2. Authentication Load Test

```bash
cd "C:\Users\shaki\IdeaProjects\dragon-of-north\load_test"
k6 run auth-load-test.js
```

### 3. Protected Endpoint Concurrency Test

```powershell
# Windows PowerShell - Set token first
$env:ACCESS_TOKEN="your_access_token_here"
k6 run protected-concurrency-test.js
```

```bash
# Linux/Mac - Set token first
export ACCESS_TOKEN="your_access_token_here"
k6 run protected-concurrency-test.js
```

---

## Metrics to Record for Resume

### Primary Metrics (Always Include)

| Metric                       | What It Shows              | Good Values                           |
|------------------------------|----------------------------|---------------------------------------|
| **http_reqs** (Requests/sec) | Throughput capacity        | Higher is better                      |
| **http_req_duration** (p95)  | Latency at 95th percentile | < 500ms for health, < 1000ms for auth |
| **http_req_failed** (Rate)   | Error percentage           | < 1% for health, < 5% for auth        |
| **vus_max**                  | Peak concurrent users      | Shows load test scale                 |

### Secondary Metrics (Including If Impressive)

| Metric                      | What It Shows                |
|-----------------------------|------------------------------|
| **http_req_duration** (p99) | Worst-case latency           |
| **iterations**              | Total successful test cycles |
| **data_received**           | Throughput in bytes          |

---

## k6 Output Example

```
  █ TOTAL RESULTS 

    HTTP
    http_req_duration....: avg=45.12ms  min=23ms  med=41ms  max=156ms  p(90)=58ms  p(95)=72ms 
    http_req_failed......: 0.00% ✓ 0        ✗ 1452    
    http_reqs............: 1452    16.13/s

    EXECUTION
    iteration_duration...: avg=1.12s  min=1.05s  med=1.1s  max=1.56s  p(90)=1.18s  p(95)=1.25s
    iterations...........: 1452    16.13/s
    vus..................: 40      min=10       max=40
    vus_max..............: 40      min=10       max=40
```

---

## Resume Bullet Points Template

**Example – Health Endpoint Test:**
> "Conducted production-safe load testing on AWS EC2 infrastructure using k6, achieving 16.13 req/sec throughput with 72
> ms p95 latency under 40 concurrent users, maintaining 0% error rate over 90-second test duration."

**Example - Authentication Test:**
> "Validated authentication API resilience under load with Redis rate limiting, sustaining 30 concurrent login requests
> with 95% of responses under 1000 ms while maintaining sub-5% error tolerance."

**Example – Protected Endpoint Test:**
> "Tested session management concurrency with authenticated requests, achieving 600 ms p95 latency for protected
> endpoints under 25 concurrent users with cookie-based JWT authentication."

---

## Safety Features Built-In

1. **Gradual Stages**: No sudden traffic spikes that could crash production
2. **Conservative VU Limits**: Max 40 VUs (health), 30 VUs (auth), 25 VUs (protected)
3. **Short Duration**: All tests complete in under 2 minutes
4. **Think Time**: Random sleep between requests to simulate realistic user behavior
5. **Thresholds**: Tests auto-fail if latency or error rates exceed safe bounds

---

## Test Descriptions

| Test File                       | Endpoint                           | Peak VUs | Duration | Purpose                            |
|---------------------------------|------------------------------------|----------|----------|------------------------------------|
| `health-stability-test.js`      | GET /actuator/health               | 40       | ~90s     | Infrastructure stability           |
| `auth-load-test.js`             | POST /api/v1/auth/identifier/login | 30       | ~105s    | Login API under load               |
| `protected-concurrency-test.js` | GET /api/v1/sessions/get/all       | 25       | ~100s    | Authenticated endpoint concurrency |

---

## Getting an Access Token (for Protected Test)

```bash
# Login via API to get token
curl -X POST https://dragon-api.duckdns.org/api/v1/auth/identifier/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"your.email@example.com","password":"your password","device_id":"test-device"}' \
  -c cookies.txt

# Extract token from cookies (access_token cookie)
```

Or manually copy the `access_token` cookie value from browser dev tools after logging in.

---

## Additional Authentication Flow Tests

These scripts model full browser-style auth flow using cookies from `Set-Cookie`.

### Required environment variables

```bash
export EMAIL="your.email@example.com"
export PASSWORD="your-password"
```

### Run

```bash
k6 run login-load-test.js
k6 run refresh-storm-test.js
k6 run session-read-test.js
k6 run logout-test.js
k6 run multi-device-session-test.js
```

### Optional manual cookie simulation

Use these for previously authenticated user simulation:

```bash
export ACCESS_COOKIE="<access_token_cookie_value>"
export REFRESH_COOKIE="<refresh_token_cookie_value>"
```
