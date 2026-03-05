import http from "k6/http";
import {check, sleep} from "k6";
import {Rate, Trend} from "k6/metrics";
import {
    BASE_URL,
    applyManualCookies,
    captureAuthCookies,
    loginAndCaptureCookies,
    requireCredentials,
} from "./auth-cookie-utils.js";

const refreshSuccessRate = new Rate("refresh_success_rate");
const refreshUnauthorizedRate = new Rate("refresh_unauthorized_rate");
const refreshLatency = new Trend("refresh_latency");

export const options = {
    stages: [
        {duration: "15s", target: 8},
        {duration: "20s", target: 20},
        {duration: "35s", target: 20},
        {duration: "15s", target: 0},
    ],
    thresholds: {
        refresh_latency: ["p(95)<1200", "avg<700"],
        refresh_success_rate: ["rate>0.70"],
    },
};

export function setup() {
    requireCredentials();
}

export default function () {
    const jar = http.cookieJar();
    const deviceId = `refresh-storm-vu-${__VU}`;

    const manual = applyManualCookies(jar);

    if (!manual.hasManualRefreshCookie) {
        const loginResult = loginAndCaptureCookies(deviceId, jar);
        if (loginResult.response.status !== 200) {
            sleep(1);
            return;
        }
    }

    const response = http.post(
        `${BASE_URL}/api/v1/auth/jwt/refresh`,
        JSON.stringify({device_id: deviceId}),
        {
            headers: {"Content-Type": "application/json", Accept: "application/json"},
            jar,
            tags: {endpoint: "refresh"},
        },
    );

    captureAuthCookies(response, jar);
    refreshLatency.add(response.timings.duration);
    refreshSuccessRate.add(response.status === 200);
    refreshUnauthorizedRate.add(response.status === 401);

    check(response, {
        "refresh status is valid": (r) => [200, 401, 429].includes(r.status),
        "refresh not server error": (r) => r.status < 500,
    });

    sleep(Math.random() * 1.2 + 0.3);
}
