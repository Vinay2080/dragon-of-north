import {check, sleep} from "k6";
import {Rate, Trend} from "k6/metrics";
import http from "k6/http";
import {BASE_URL, captureAuthCookies, requireCredentials, EMAIL, PASSWORD} from "./auth-cookie-utils.js";

const loginSuccessRate = new Rate("login_success_rate");
const loginCookieCaptureRate = new Rate("login_cookie_capture_rate");
const loginLatency = new Trend("login_latency");

export const options = {
    stages: [
        {duration: "20s", target: 10},
        {duration: "30s", target: 25},
        {duration: "40s", target: 25},
        {duration: "20s", target: 5},
        {duration: "10s", target: 0},
    ],
    thresholds: {
        login_latency: ["p(95)<1200", "avg<700"],
        login_success_rate: ["rate>0.80"],
    },
};

export function setup() {
    requireCredentials();
}

export default function () {
    const jar = http.cookieJar();
    const deviceId = `login-load-vu-${__VU}-iter-${__ITER}`;

    const response = http.post(
        `${BASE_URL}/api/v1/auth/identifier/login`,
        JSON.stringify({
            identifier: EMAIL,
            password: PASSWORD,
            device_id: deviceId,
        }),
        {
            headers: {"Content-Type": "application/json", Accept: "application/json"},
            jar,
            tags: {endpoint: "login"},
        },
    );

    const {accessToken, refreshToken} = captureAuthCookies(response, jar);

    loginLatency.add(response.timings.duration);
    loginSuccessRate.add(response.status === 200);
    loginCookieCaptureRate.add(Boolean(accessToken) || Boolean(refreshToken));

    check(response, {
        "login status is valid": (r) => [200, 401, 429].includes(r.status),
        "login is not server error": (r) => r.status < 500,
        "login latency < 1500ms": (r) => r.timings.duration < 1500,
    });

    sleep(Math.random() * 1.5 + 0.5);
}
