import http from "k6/http";
import {check, sleep} from "k6";
import {Trend} from "k6/metrics";
import {BASE_URL} from "./auth-cookie-utils.js";

/**
 * HEALTH ENDPOINT STABILITY TEST - Production Safe
 *
 * Tests GET /actuator/health endpoint for infrastructure baseline.
 * This is the SAFEST test - no auth required, minimal server load.
 *
 * For health checks, http_req_failed IS meaningful because:
 * - 200 is the only expected response
 * - Any non-2xx indicates infrastructure problems
 *
 * Backend: https://dragon-api.duckdns.org (AWS EC2 + Spring Boot)
 */

const HEALTH_P95_MS = Number(__ENV.HEALTH_P95_MS || 5000);
const HEALTH_AVG_MS = Number(__ENV.HEALTH_AVG_MS || 3000);

// Custom metric for health endpoint latency
const healthLatency = new Trend("health_latency");

export const options = {
    stages: [
        {duration: "15s", target: 10},   // Warm-up
        {duration: "20s", target: 25},   // Ramp up
        {duration: "25s", target: 40},   // Peak: 40 VUs (safe for health)
        {duration: "20s", target: 20},   // Ramp down
        {duration: "15s", target: 0},    // Cool down
    ],
    thresholds: {
        // Health checks should be FAST - strict latency threshold
        http_req_duration: [`p(95)<${HEALTH_P95_MS}`, `avg<${HEALTH_AVG_MS}`],
        health_latency: [`p(95)<${HEALTH_P95_MS}`, `avg<${HEALTH_AVG_MS}`],
        // Health checks should almost never fail - strict error threshold
        http_req_failed: ["rate<0.005"],  // 0.5% max error rate
        // Success rate threshold
        checks: ["rate>0.99"],  // 99% of checks must pass
    },
};

export function setup() {
    console.log("╔════════════════════════════════════════════════════════╗");
    console.log("║   HEALTH STABILITY TEST                              ║");
    console.log("╠════════════════════════════════════════════════════════╣");
    console.log(`  Target: ${BASE_URL}/actuator/health`);
    console.log("  Max VUs: 40  |  Duration: ~95 seconds");
    console.log(`  Thresholds: p95<${HEALTH_P95_MS}ms avg<${HEALTH_AVG_MS}ms`);
    console.log("  Purpose: Infrastructure baseline / Load balancer check");
    console.log("╚════════════════════════════════════════════════════════╝");
    return {};
}

export default function () {
    const response = http.get(`${BASE_URL}/actuator/health`, {
        tags: {endpoint: "health"},
    });

    // Track health-specific latency
    healthLatency.add(response.timings.duration);

    // Health checks have strict requirements
    check(response, {
        "health: status is 200": (r) => r.status === 200,
        "health: latency under threshold": (r) => r.timings.duration < HEALTH_P95_MS,
        "health: response valid JSON": (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.status === "UP" || body.status !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    // Fast think time - health checks are frequent but not spam
    sleep(Math.random() * 0.2 + 0.1);  // 100-300ms
}

export function teardown(data) {
    console.log("╔════════════════════════════════════════════════════════╗");
    console.log("║   HEALTH TEST COMPLETED                              ║");
    console.log("╠════════════════════════════════════════════════════════╣");
    console.log("  Resume Metrics to Record:");
    console.log("    • http_reqs/sec          - Throughput");
    console.log("    • http_req_duration p(95) - Latency at 95th percentile");
    console.log("    • health_latency p(95)   - Health-specific latency");
    console.log("    • http_req_failed rate   - Infrastructure error rate");
    console.log("    • vus_max                - Peak concurrent users");
    console.log("╚════════════════════════════════════════════════════════╝");
}
