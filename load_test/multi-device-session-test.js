import http from "k6/http";
import {check, sleep} from "k6";
import {Rate, Trend} from "k6/metrics";
import {
    BASE_URL,
    captureAuthCookies,
    requireCredentials,
    EMAIL,
    PASSWORD,
} from "./auth-cookie-utils.js";

const multiDeviceLoginSuccessRate = new Rate("multi_device_login_success_rate");
const sessionListSuccessRate = new Rate("multi_device_session_list_success_rate");
const multiDeviceFlowLatency = new Trend("multi_device_flow_latency");

const DEVICE_COUNT = Number(__ENV.DEVICE_COUNT || 3);

export const options = {
    stages: [
        {duration: "20s", target: 4},
        {duration: "20s", target: 8},
        {duration: "30s", target: 8},
        {duration: "10s", target: 0},
    ],
    thresholds: {
        multi_device_flow_latency: ["p(95)<2000"],
    },
};

export function setup() {
    requireCredentials();
}

function loginForDevice(jar, deviceId) {
    const loginResponse = http.post(
        `${BASE_URL}/api/v1/auth/identifier/login`,
        JSON.stringify({
            identifier: EMAIL,
            password: PASSWORD,
            device_id: deviceId,
        }),
        {
            headers: {"Content-Type": "application/json", Accept: "application/json"},
            jar,
            tags: {endpoint: "multi-device-login"},
        },
    );

    captureAuthCookies(loginResponse, jar);
    return loginResponse;
}

export default function () {
    const startedAt = Date.now();
    const jars = [];
    let successfulLogins = 0;

    for (let i = 0; i < DEVICE_COUNT; i += 1) {
        const jar = http.cookieJar();
        jars.push(jar);

        const deviceId = `multi-device-vu-${__VU}-iter-${__ITER}-dev-${i}`;
        const loginResponse = loginForDevice(jar, deviceId);
        const isSuccess = loginResponse.status === 200;
        successfulLogins += isSuccess ? 1 : 0;
        multiDeviceLoginSuccessRate.add(isSuccess);

        check(loginResponse, {
            [`device ${i} login status valid`]: (r) => [200, 401, 429].includes(r.status),
            [`device ${i} login not server error`]: (r) => r.status < 500,
        });

        sleep(Math.random() * 0.5 + 0.2);
    }

    if (successfulLogins === 0) {
        sleep(1);
        return;
    }

    const sessionResponse = http.get(`${BASE_URL}/api/v1/sessions/get/all`, {
        headers: {Accept: "application/json"},
        jar: jars[0],
        tags: {endpoint: "multi-device-session-list"},
    });

    sessionListSuccessRate.add(sessionResponse.status === 200);
    check(sessionResponse, {
        "multi-device session list status valid": (r) => [200, 401, 403, 429].includes(r.status),
        "multi-device session list not server error": (r) => r.status < 500,
    });

    multiDeviceFlowLatency.add(Date.now() - startedAt);
    sleep(Math.random() * 1.2 + 0.8);
}
