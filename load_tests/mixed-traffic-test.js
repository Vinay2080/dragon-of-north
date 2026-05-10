import http from "k6/http";
import {check, sleep} from "k6";
import {Rate, Trend} from "k6/metrics";
import {applyManualCookies, BASE_URL, buildDeviceId, loginAndCaptureCookies,} from "./auth-cookie-utils.js";

/**
 * MIXED TRAFFIC LOAD TEST - Production Safe
 *
 * Updated to reflect new features:
 * - Adds Profile reads to the protected traffic mix (GET /api/v1/profile)
 * - Removes hardcoded JWTs; uses cookie-jar auth (manual cookies or per-VU login)
 */

// Custom metrics for each endpoint
const healthLatency = new Trend("health_latency");
const loginLatency = new Trend("login_latency");
const sessionsLatency = new Trend("sessions_latency");
const profileLatency = new Trend("profile_latency");

const loginSuccessRate = new Rate("login_success_rate");
const loginRateLimitedRate = new Rate("login_rate_limited_rate");

const sessionsSuccessRate = new Rate("sessions_success_rate");
const sessionsAuthErrorRate = new Rate("sessions_auth_error_rate");

const profileSuccessRate = new Rate("profile_success_rate");
const profileAuthErrorRate = new Rate("profile_auth_error_rate");

const anyServerErrorRate = new Rate("any_server_error_rate");

const HAS_CREDS = Boolean(__ENV.EMAIL) && Boolean(__ENV.PASSWORD);

const MIXED_P95_MS = Number(__ENV.MIXED_P95_MS || 6000);
const MIXED_AVG_MS = Number(__ENV.MIXED_AVG_MS || 3500);

export const options = {
    stages: [
        {duration: "15s", target: 10},   // Warm-up
        {duration: "25s", target: 25},   // Ramp up
        {duration: "30s", target: 40},   // Peak: 40 VUs
        {duration: "20s", target: 15},   // Ramp down
        {duration: "15s", target: 0},    // Cool down
    ],
    thresholds: {
        // Overall latency (across all endpoints)
        http_req_duration: [`p(95)<${MIXED_P95_MS}`],
        // Endpoint-specific latency thresholds
        health_latency: [`p(95)<${MIXED_P95_MS}`, `avg<${MIXED_AVG_MS}`],
        login_latency: [`p(95)<${MIXED_P95_MS}`, `avg<${MIXED_AVG_MS}`],
        sessions_latency: [`p(95)<${MIXED_P95_MS}`, `avg<${MIXED_AVG_MS}`],
        profile_latency: [`p(95)<${MIXED_P95_MS}`, `avg<${MIXED_AVG_MS}`],
        // Note: No http_req_failed threshold - misleading for mixed traffic
    },
};

export function setup() {
    console.log("╔════════════════════════════════════════════════════════╗");
    console.log("║   MIXED TRAFFIC LOAD TEST                            ║");
    console.log("╠════════════════════════════════════════════════════════╣");
    console.log(`  Target: ${BASE_URL}`);
    console.log("  Traffic Split: 20% Health | 30% Auth | 30% Sessions | 20% Profile");
    console.log("  Max VUs: 40  |  Duration: ~105 seconds");
    console.log("╠════════════════════════════════════════════════════════╣");

    if (__ENV.ACCESS_COOKIE) {
        console.log("✓ ACCESS_COOKIE provided (manual cookie mode)");
    } else if (HAS_CREDS) {
        console.log("✓ EMAIL/PASSWORD provided (per-VU login mode)");
    } else {
        console.log("⚠ No ACCESS_COOKIE and no EMAIL/PASSWORD. Protected endpoints will likely return 401/403.");
    }

    console.log("╚════════════════════════════════════════════════════════╝");
    sleep(2);

    return {};
}

// Per-VU state
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

    const deviceId = buildDeviceId(`mixed-traffic-login-${__VU}`);
    const loginResult = loginAndCaptureCookies(deviceId, jar);
    authPrepared = true;

    // Record a single outcome (useful if login is heavily rate-limited)
    loginSuccessRate.add(loginResult.response.status === 200);
    loginRateLimitedRate.add(loginResult.response.status === 429);
}

export default function () {
    // Weighted random selection: 0-20 (health), 20-50 (login), 50-80 (sessions), 80-100 (profile)
    const random = Math.random() * 100;

    // ═══════════════════════════════════════════════════════════
    // 20% TRAFFIC: Health Check
    // ═══════════════════════════════════════════════════════════
    if (random < 20) {
        const response = http.get(`${BASE_URL}/actuator/health`, {
            tags: {endpoint: "health"},
        });

        healthLatency.add(response.timings.duration);

        check(response, {
            "health: status 200": (r) => r.status === 200,
            "health: latency under threshold": (r) => r.timings.duration < MIXED_P95_MS,
        });

        anyServerErrorRate.add(response.status >= 500);
    }
        // ═══════════════════════════════════════════════════════════
        // 30% TRAFFIC: Authentication (Login)
        // Expected: 200 (success), 401 (invalid creds), 429 (rate limited)
    // ═══════════════════════════════════════════════════════════
    else if (random < 50) {
        if (!HAS_CREDS) {
            // If no credentials are configured, don't spam invalid login attempts.
            sleep(0.1);
            return;
        }

        const deviceId = buildDeviceId(`mixed-traffic-login-${__VU}-${__ITER}`);
        const response = http.post(
            `${BASE_URL}/api/v1/auth/identifier/login`,
            JSON.stringify({
                identifier: __ENV.EMAIL,
                password: __ENV.PASSWORD,
                device_id: deviceId,
            }),
            {
                headers: {"Content-Type": "application/json", Accept: "application/json"},
                jar,
                tags: {endpoint: "login"},
            },
        );

        loginLatency.add(response.timings.duration);
        loginSuccessRate.add(response.status === 200);
        loginRateLimitedRate.add(response.status === 429);

        check(response, {
            "login: valid status (200/401/429)": (r) => [200, 401, 429].includes(r.status),
            "login: not server error": (r) => r.status < 500,
        });

        anyServerErrorRate.add(response.status >= 500);
    }
        // ═══════════════════════════════════════════════════════════
        // 50% TRAFFIC: Protected Endpoint (Sessions)
        // Expected: 200 (success), 401/403 (auth error), 429 (rate limited)
    // ═══════════════════════════════════════════════════════════
    else if (random < 80) {
        ensureAuth();

        const response = http.get(`${BASE_URL}/api/v1/sessions/get/all`, {
            headers: {Accept: "application/json"},
            jar,
            tags: {endpoint: "sessions"},
        });

        sessionsLatency.add(response.timings.duration);
        sessionsSuccessRate.add(response.status === 200);
        sessionsAuthErrorRate.add(response.status === 401 || response.status === 403);

        check(response, {
            "sessions: valid status (200/401/403/429)": (r) => [200, 401, 403, 429].includes(r.status),
            "sessions: not server error": (r) => r.status < 500,
        });

        anyServerErrorRate.add(response.status >= 500);
    } else {
        ensureAuth();

        const response = http.get(`${BASE_URL}/api/v1/profile`, {
            headers: {Accept: "application/json"},
            jar,
            tags: {endpoint: "profile"},
        });

        profileLatency.add(response.timings.duration);
        profileSuccessRate.add(response.status === 200);
        profileAuthErrorRate.add(response.status === 401 || response.status === 403);

        check(response, {
            "profile: valid status (200/401/403/429)": (r) => [200, 401, 403, 429].includes(r.status),
            "profile: not server error": (r) => r.status < 500,
        });

        anyServerErrorRate.add(response.status >= 500);
    }

    // Realistic think time between user actions
    sleep(Math.random() * 0.4 + 0.2);  // 200-600ms
}

export function teardown(data) {
    console.log("╔════════════════════════════════════════════════════════╗");
    console.log("║   MIXED TRAFFIC TEST COMPLETED                       ║");
    console.log("╠════════════════════════════════════════════════════════╣");
    console.log("  Resume Metrics to Record:");
    console.log("    • http_reqs/sec              - Overall throughput");
    console.log("    • http_req_duration p(95)     - Overall latency");
    console.log("    • health_latency p(95)       - Infrastructure latency");
    console.log("    • login_latency p(95)        - Auth endpoint latency");
    console.log("    • sessions_latency p(95)     - Sessions endpoint latency");
    console.log("    • profile_latency p(95)      - Profile endpoint latency");
    console.log("    • login_success_rate         - Auth success %");
    console.log("    • sessions_success_rate      - Sessions success %");
    console.log("    • profile_success_rate       - Profile success %");
    console.log("    • vus_max                    - Peak concurrent users");
    console.log("╚════════════════════════════════════════════════════════╝");
}
