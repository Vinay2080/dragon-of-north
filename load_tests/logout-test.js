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

const logoutSuccessRate = new Rate("logout_success_rate");
const logoutLatency = new Trend("logout_latency");

const LOGOUT_P95_MS = Number(__ENV.LOGOUT_P95_MS || 6000);
const LOGOUT_AVG_MS = Number(__ENV.LOGOUT_AVG_MS || 3500);

export const options = {
    stages: [
        {duration: "15s", target: 5},
        {duration: "20s", target: 12},
        {duration: "25s", target: 12},
        {duration: "15s", target: 0},
    ],
    thresholds: {
        logout_latency: [`p(95)<${LOGOUT_P95_MS}`, `avg<${LOGOUT_AVG_MS}`],
    },
};

export function setup() {
    requireCredentials();
}

export default function () {
    const jar = http.cookieJar();
    const deviceId = buildDeviceId(`logout-${__VU}-${__ITER}`);

    const manual = applyManualCookies(jar);
    if (!manual.hasManualAccessCookie) {
        const loginResult = loginAndCaptureCookies(deviceId, jar);
        if (loginResult.response.status !== 200) {
            sleep(1);
            return;
        }
    }

    const response = http.post(
        `${BASE_URL}/api/v1/auth/identifier/logout`,
        JSON.stringify({device_id: deviceId}),
        {
            headers: {"Content-Type": "application/json", Accept: "application/json"},
            jar,
            tags: {endpoint: "logout"},
        },
    );

    logoutLatency.add(response.timings.duration);
    logoutSuccessRate.add(response.status === 200);

    check(response, {
        "logout status is valid": (r) => [200, 401, 403, 429].includes(r.status),
        "logout not server error": (r) => r.status < 500,
    });

    sleep(Math.random() * 1.5 + 0.5);
}
