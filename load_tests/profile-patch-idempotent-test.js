import http from "k6/http";
import {check, sleep} from "k6";
import {Rate, Trend} from "k6/metrics";
import {
    applyManualCookies,
    BASE_URL,
    buildCsrfHeaders,
    buildDeviceId,
    fetchCsrfToken,
    loginAndCaptureCookies,
    requireCredentials,
} from "./auth-cookie-utils.js";

/**
 * PROFILE PATCH (IDEMPOTENT) TEST
 *
 * Covers new CSRF-protected mutating request path:
 * - PATCH /api/v1/profile (requires XSRF-TOKEN cookie + X-XSRF-TOKEN header)
 *
 * Safety:
 * - By default this test SKIPS the PATCH to avoid unintended writes.
 * - Enable by setting: ENABLE_MUTATIONS=1
 */

const ENABLE_MUTATIONS = __ENV.ENABLE_MUTATIONS === "1";

const profileGetSuccessRate = new Rate("profile_get_success_rate");
const profilePatchSuccessRate = new Rate("profile_patch_success_rate");
const profilePatchSkippedRate = new Rate("profile_patch_skipped_rate");

const profileGetLatency = new Trend("profile_get_latency");
const profilePatchLatency = new Trend("profile_patch_latency");

const PROFILE_GET_P95_MS = Number(__ENV.PROFILE_GET_P95_MS || 6000);
const PROFILE_PATCH_P95_MS = Number(__ENV.PROFILE_PATCH_P95_MS || 8000);

export const options = {
    stages: [
        {duration: "10s", target: 2},
        {duration: "20s", target: 5},
        {duration: "20s", target: 5},
        {duration: "10s", target: 0},
    ],
    thresholds: {
        profile_get_latency: [`p(95)<${PROFILE_GET_P95_MS}`],
        // Only meaningful when ENABLE_MUTATIONS=1
        profile_patch_latency: [`p(95)<${PROFILE_PATCH_P95_MS}`],
    },
};

export function setup() {
    if (!__ENV.ACCESS_COOKIE) {
        requireCredentials();
    }

    if (!ENABLE_MUTATIONS) {
        console.log("[profile-patch-idempotent-test] ENABLE_MUTATIONS is not set; PATCH will be skipped.");
    }
}

export default function () {
    const jar = http.cookieJar();
    const deviceId = buildDeviceId(`profile-patch-${__VU}-${__ITER}`);

    const manual = applyManualCookies(jar);
    if (!manual.hasManualAccessCookie) {
        const loginResult = loginAndCaptureCookies(deviceId, jar);
        if (loginResult.response.status !== 200) {
            sleep(1);
            return;
        }
    }

    // Step 1: Read current profile (so PATCH can be idempotent)
    const profileGet = http.get(`${BASE_URL}/api/v1/profile`, {
        headers: {Accept: "application/json"},
        jar,
        tags: {endpoint: "profile-get"},
    });

    profileGetLatency.add(profileGet.timings.duration);
    profileGetSuccessRate.add(profileGet.status === 200);

    check(profileGet, {
        "profile-get status valid": (r) => [200, 401, 403, 429].includes(r.status),
        "profile-get not server error": (r) => r.status < 500,
    });

    if (!ENABLE_MUTATIONS || profileGet.status !== 200) {
        profilePatchSkippedRate.add(true);
        sleep(Math.random() * 1.0 + 0.5);
        return;
    }

    // Step 2: Obtain CSRF token
    const {token: csrfToken, response: csrfResponse} = fetchCsrfToken(jar);
    if (csrfResponse.status !== 200 || !csrfToken) {
        profilePatchSkippedRate.add(true);
        sleep(Math.random() * 1.0 + 0.5);
        return;
    }

    let patchPayload = {};
    try {
        const parsed = JSON.parse(profileGet.body || "{}");
        const data = parsed?.data || {};
        // Send the same values back (idempotent intent)
        patchPayload = {
            username: data.username,
            display_name: data.display_name,
            bio: data.bio,
            avatar_url: data.avatar_url,
        };

        // Remove undefined/null keys to avoid validation surprises
        Object.keys(patchPayload).forEach((k) => {
            if (patchPayload[k] === undefined || patchPayload[k] === null) {
                delete patchPayload[k];
            }
        });
    } catch (_) {
        profilePatchSkippedRate.add(true);
        sleep(Math.random() * 1.0 + 0.5);
        return;
    }

    const profilePatch = http.patch(
        `${BASE_URL}/api/v1/profile`,
        JSON.stringify(patchPayload),
        {
            headers: buildCsrfHeaders(csrfToken, {
                "Content-Type": "application/json",
                Accept: "application/json",
            }),
            jar,
            tags: {endpoint: "profile-patch"},
        },
    );

    profilePatchLatency.add(profilePatch.timings.duration);
    profilePatchSuccessRate.add(profilePatch.status === 200);

    check(profilePatch, {
        "profile-patch status valid": (r) => [200, 400, 401, 403, 429].includes(r.status),
        "profile-patch not server error": (r) => r.status < 500,
    });

    sleep(Math.random() * 1.2 + 0.6);
}


