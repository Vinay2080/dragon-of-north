import http from "k6/http";
import {check, sleep} from "k6";
import {Rate, Trend} from "k6/metrics";
import {
    BASE_URL,
    applyManualCookies,
    loginAndCaptureCookies,
    requireCredentials,
} from "./auth-cookie-utils.js";

const sessionsReadSuccessRate = new Rate("sessions_read_success_rate");
const sessionsAuthFailureRate = new Rate("sessions_auth_failure_rate");
const sessionsLatency = new Trend("sessions_read_latency");

export const options = {
    stages: [
        {duration: "20s", target: 10},
        {duration: "25s", target: 20},
        {duration: "30s", target: 20},
        {duration: "15s", target: 0},
    ],
    thresholds: {
        sessions_read_latency: ["p(95)<1000", "avg<600"],
    },
};

export function setup() {
    requireCredentials();
}

export default function () {
    const jar = http.cookieJar();
    const deviceId = `session-read-vu-${__VU}`;

    const manual = applyManualCookies(jar);
    if (!manual.hasManualAccessCookie) {
        const loginResult = loginAndCaptureCookies(deviceId, jar);
        if (loginResult.response.status !== 200) {
            sleep(1);
            return;
        }
    }

    const response = http.get(`${BASE_URL}/api/v1/sessions/get/all`, {
        headers: {Accept: "application/json"},
        jar,
        tags: {endpoint: "sessions-read"},
    });

    sessionsLatency.add(response.timings.duration);
    sessionsReadSuccessRate.add(response.status === 200);
    sessionsAuthFailureRate.add(response.status === 401 || response.status === 403);

    check(response, {
        "sessions endpoint returned expected status": (r) => [200, 401, 403, 429].includes(r.status),
        "sessions endpoint not server error": (r) => r.status < 500,
    });

    sleep(Math.random() * 2 + 0.5);
}
