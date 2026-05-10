import http from "k6/http";
import {check, sleep} from "k6";
import {Rate, Trend} from "k6/metrics";
import {applyManualCookies, BASE_URL, buildDeviceId, loginAndCaptureCookies,} from "./auth-cookie-utils.js";

const PROTECTED_P95_MS = Number(__ENV.PROTECTED_P95_MS || 5000);
const PROTECTED_AVG_MS = Number(__ENV.PROTECTED_AVG_MS || 3000);

/**
 * PROTECTED ENDPOINT CONCURRENCY TEST - Production Safe
 *
 * Tests GET /api/v1/sessions/get/all with authenticated requests.
 * This endpoint requires a valid JWT token in the access_token cookie.
 *
 * For protected endpoints, http_req_failed IS misleading because:
 * - 401 (unauthorized) is expected without valid token
 * - 403 (forbidden) is valid for insufficient permissions
 * - 429 (rate limited) is expected Redis behavior
 *
 * We use custom Rate metrics to track actual outcome distribution.
 *
 * Backend: https://dragon-api.duckdns.org (AWS EC2 + Spring Boot + Redis)
 */

// Custom metrics for accurate tracking
const protectedSuccessRate = new Rate("protected_success_rate");
const protectedAuthErrorRate = new Rate("protected_auth_error_rate");
const protectedRateLimitedRate = new Rate("protected_rate_limited_rate");
const protectedLatency = new Trend("protected_latency");

const HAS_CREDS = Boolean(__ENV.EMAIL) && Boolean(__ENV.PASSWORD);

export const options = {
    stages: [
        {duration: "15s", target: 10},   // Warm-up
        {duration: "25s", target: 25},   // Ramp to peak
        {duration: "30s", target: 25},   // Sustained peak
        {duration: "20s", target: 10},   // Ramp down
        {duration: "15s", target: 0},    // Cool down
    ],
    thresholds: {
        // Protected endpoints should be reasonably fast
        http_req_duration: [`p(95)<${PROTECTED_P95_MS}`],
        protected_latency: [`p(95)<${PROTECTED_P95_MS}`, `avg<${PROTECTED_AVG_MS}`],
        // Note: We do NOT use http_req_failed - 401/403/429 are valid outcomes
    },
};

export function setup() {
    console.log("╔════════════════════════════════════════════════════════╗");
    console.log("║   PROTECTED ENDPOINT CONCURRENCY TEST                ║");
    console.log("╠════════════════════════════════════════════════════════╣");
    console.log(`  Target: ${BASE_URL}/api/v1/sessions/get/all`);
    console.log("  Max VUs: 25  |  Duration: ~105 seconds");
    console.log("  Auth Method: Cookie (access_token)");
    console.log("╚════════════════════════════════════════════════════════╝");

    if (__ENV.ACCESS_COOKIE) {
        console.log("✓ ACCESS_COOKIE provided (manual cookie mode)");
    } else if (HAS_CREDS) {
        console.log("✓ EMAIL/PASSWORD provided (per-VU login mode)");
    } else {
        console.log("⚠ No ACCESS_COOKIE and no EMAIL/PASSWORD. Expected: 401/403 responses (auth rejection path)");
    }

    return {};
}

// Per-VU state (each VU has its own JS runtime)
const jar = http.cookieJar();
let authPrepared = false;

function ensureAuth() {
    if (authPrepared) {
        return;
    }

    const manual = applyManualCookies(jar);
    if (manual.hasManualAccessCookie) {
        authPrepared = true;
        return;
    }

    if (!HAS_CREDS) {
        authPrepared = true;
        return;
    }

    const deviceId = buildDeviceId(`protected-concurrency-login-${__VU}`);
    const loginResult = loginAndCaptureCookies(deviceId, jar);
    // Even if login is rate-limited or unauthorized, we mark prepared to avoid retry storms.
    authPrepared = true;

    check(loginResult.response, {
        "protected setup login: status valid": (r) => [200, 401, 429].includes(r.status),
        "protected setup login: not server error": (r) => r.status < 500,
    });
}

export default function () {
    ensureAuth();

    const response = http.get(`${BASE_URL}/api/v1/sessions/get/all`, {
        headers: {Accept: "application/json"},
        jar,
        tags: {endpoint: "protected-sessions"},
    });

    // Track endpoint-specific latency
    protectedLatency.add(response.timings.duration);

    // Track outcome distribution using custom metrics
    protectedSuccessRate.add(response.status === 200);
    protectedAuthErrorRate.add(response.status === 401 || response.status === 403);
    protectedRateLimitedRate.add(response.status === 429);

    // Valid responses: 200 (success), 401/403 (auth), 429 (rate limit)
    check(response, {
        "protected: received response": (r) => r.status !== 0,
        "protected: valid status (200/401/403/429)": (r) =>
            [200, 401, 403, 429].includes(r.status),
        "protected: latency under threshold": (r) => r.timings.duration < PROTECTED_P95_MS,
        "protected: not server error": (r) => r.status < 500,
    });

    // Additional check for successful requests
    if (response.status === 200) {
        check(response, {
            "protected: has response body": (r) => r.body && r.body.length > 0,
        });
    }

    // Realistic think time between session requests
    sleep(Math.random() * 0.4 + 0.2);  // 200-600ms
}

export function teardown(data) {
    console.log("╔════════════════════════════════════════════════════════╗");
    console.log("║   PROTECTED ENDPOINT TEST COMPLETED                  ║");
    console.log("╠════════════════════════════════════════════════════════╣");
    console.log("  Resume Metrics to Record:");
    console.log("    • http_reqs/sec           - Throughput");
    console.log("    • http_req_duration p(95)  - Overall latency");
    console.log("    • protected_latency p(95)  - Protected endpoint latency");
    console.log("    • protected_success_rate   - % of 200 responses");
    console.log("    • protected_auth_error_rate - % of 401/403 responses");
    console.log("    • vus_max                 - Peak concurrent users");
    console.log("╚════════════════════════════════════════════════════════╝");
}
