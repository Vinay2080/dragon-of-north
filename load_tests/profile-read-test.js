import http from "k6/http";
import {check, sleep} from "k6";
import {Rate, Trend} from "k6/metrics";
import {
    applyManualCookies,
    BASE_URL,
    buildDeviceId,
    loginAndCaptureCookies,
    requireCredentials,
} from "./auth-cookie-utils.js";

/**
 * PROFILE READ LOAD TEST (production-safe)
 *
 * New feature coverage:
 * - GET /api/v1/profile
 *
 * Auth required. Uses cookie-jar auth:
 * - Prefer ACCESS_COOKIE if provided.
 * - Otherwise logs in with EMAIL/PASSWORD.
 */

const profileSuccessRate = new Rate("profile_success_rate");
const profileAuthErrorRate = new Rate("profile_auth_error_rate");
const profileLatency = new Trend("profile_latency");

const PROFILE_P95_MS = Number(__ENV.PROFILE_P95_MS || 6000);
const PROFILE_AVG_MS = Number(__ENV.PROFILE_AVG_MS || 3500);

export const options = {
    stages: [
        {duration: "15s", target: 5},
        {duration: "20s", target: 12},
        {duration: "25s", target: 12},
        {duration: "15s", target: 0},
    ],
    thresholds: {
        profile_latency: [`p(95)<${PROFILE_P95_MS}`, `avg<${PROFILE_AVG_MS}`],
    },
};

export function setup() {
    // For profile reads we need auth; allow manual cookies OR credentials.
    if (!__ENV.ACCESS_COOKIE) {
        requireCredentials();
    }
}

export default function () {
    const jar = http.cookieJar();
    const deviceId = buildDeviceId(`profile-read-${__VU}-${__ITER}`);

    const manual = applyManualCookies(jar);
    if (!manual.hasManualAccessCookie) {
        const loginResult = loginAndCaptureCookies(deviceId, jar);
        if (loginResult.response.status !== 200) {
            sleep(1);
            return;
        }
    }

    const response = http.get(`${BASE_URL}/api/v1/profile`, {
        headers: {Accept: "application/json"},
        jar,
        tags: {endpoint: "profile"},
    });

    profileLatency.add(response.timings.duration);
    profileSuccessRate.add(response.status === 200);
    profileAuthErrorRate.add(response.status === 401 || response.status === 403);

    check(response, {
        "profile status valid": (r) => [200, 401, 403, 429].includes(r.status),
        "profile not server error": (r) => r.status < 500,
    });

    sleep(Math.random() * 1.2 + 0.4);
}


