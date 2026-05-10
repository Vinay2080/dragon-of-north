import http from "k6/http";
import {check, sleep} from "k6";
import {Rate, Trend} from "k6/metrics";
import {BASE_URL, fetchCsrfToken} from "./auth-cookie-utils.js";

/**
 * CSRF TOKEN SMOKE / STABILITY TEST (production-safe)
 *
 * New feature coverage:
 * - GET /api/v1/auth/csrf
 * - Ensures the XSRF-TOKEN cookie (and/or body token) is returned.
 *
 * This is a safe test: it does not mutate any server-side state.
 */

const csrfSuccessRate = new Rate("csrf_success_rate");
const csrfTokenPresentRate = new Rate("csrf_token_present_rate");
const csrfLatency = new Trend("csrf_latency");

const CSRF_P95_MS = Number(__ENV.CSRF_P95_MS || 5000);
const CSRF_AVG_MS = Number(__ENV.CSRF_AVG_MS || 3000);

export const options = {
    stages: [
        {duration: "10s", target: 5},
        {duration: "20s", target: 15},
        {duration: "20s", target: 15},
        {duration: "10s", target: 0},
    ],
    thresholds: {
        csrf_latency: [`p(95)<${CSRF_P95_MS}`, `avg<${CSRF_AVG_MS}`],
        csrf_success_rate: ["rate>0.95"],
        csrf_token_present_rate: ["rate>0.90"],
    },
};

export function setup() {
    console.log("╔════════════════════════════════════════════════════════╗");
    console.log("║   CSRF TOKEN TEST                                     ║");
    console.log("╠════════════════════════════════════════════════════════╣");
    console.log(`  Target: ${BASE_URL}/api/v1/auth/csrf`);
    console.log("  Max VUs: 15  |  Duration: ~60 seconds");
    console.log("╚════════════════════════════════════════════════════════╝");
}

export default function () {
    const jar = http.cookieJar();
    const {response, token} = fetchCsrfToken(jar);

    csrfLatency.add(response.timings.duration);
    csrfSuccessRate.add(response.status === 200);
    csrfTokenPresentRate.add(Boolean(token));

    check(response, {
        "csrf returned expected status": (r) => [200, 429].includes(r.status),
        "csrf not server error": (r) => r.status < 500,
    });

    sleep(Math.random() * 0.4 + 0.2);
}


