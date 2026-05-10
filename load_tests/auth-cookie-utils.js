import http from "k6/http";
import {check} from "k6";

/**
 * Shared helpers for the load-test suite.
 *
 * Conventions:
 * - Cookie auth model: access_token + refresh_token
 * - CSRF model: XSRF-TOKEN cookie + X-XSRF-TOKEN header
 */

export const BASE_URL = __ENV.BASE_URL || "https://api.verloren.dev";
export const EMAIL = __ENV.EMAIL || "";
export const PASSWORD = __ENV.PASSWORD || "";
export const DEVICE_ID_PREFIX = __ENV.DEVICE_ID_PREFIX || "k6";

const IS_HTTPS = BASE_URL.startsWith("https://");

export function requireCredentials() {
    if (!EMAIL || !PASSWORD) {
        throw new Error(
            "EMAIL and PASSWORD environment variables are required (set them before running auth-required tests).",
        );
    }
}

export function extractCookieValue(cookieCollection, name) {
    const cookieEntries = cookieCollection && cookieCollection[name];
    if (!cookieEntries || cookieEntries.length === 0) {
        return null;
    }
    return cookieEntries[0].value;
}

export function captureAuthCookies(response, jar) {
    const accessToken = extractCookieValue(response.cookies, "access_token");
    const refreshToken = extractCookieValue(response.cookies, "refresh_token");

    if (accessToken) {
        jar.set(BASE_URL, "access_token", accessToken, {
            path: "/",
            secure: IS_HTTPS,
            http_only: true,
        });
    }

    if (refreshToken) {
        jar.set(BASE_URL, "refresh_token", refreshToken, {
            path: "/api/v1/auth/jwt/refresh",
            secure: IS_HTTPS,
            http_only: true,
        });
    }

    return {accessToken, refreshToken};
}

export function applyManualCookies(jar) {
    const manualRefreshCookie = __ENV.REFRESH_COOKIE || "";
    const manualAccessCookie = __ENV.ACCESS_COOKIE || "";

    if (manualAccessCookie) {
        jar.set(BASE_URL, "access_token", manualAccessCookie, {
            path: "/",
            secure: IS_HTTPS,
            http_only: true,
        });
    }

    if (manualRefreshCookie) {
        jar.set(BASE_URL, "refresh_token", manualRefreshCookie, {
            path: "/api/v1/auth/jwt/refresh",
            secure: IS_HTTPS,
            http_only: true,
        });
    }

    return {
        hasManualAccessCookie: Boolean(manualAccessCookie),
        hasManualRefreshCookie: Boolean(manualRefreshCookie),
    };
}

export function captureCsrfCookie(response, jar) {
    // Spring's CookieCsrfTokenRepository default cookie name.
    const csrfCookie =
        extractCookieValue(response.cookies, "XSRF-TOKEN") ||
        extractCookieValue(response.cookies, "XSRF_TOKEN");

    if (csrfCookie) {
        jar.set(BASE_URL, "XSRF-TOKEN", csrfCookie, {
            path: "/",
            secure: IS_HTTPS,
            http_only: false,
        });
    }

    return csrfCookie;
}

export function fetchCsrfToken(jar) {
    const response = http.get(`${BASE_URL}/api/v1/auth/csrf`, {
        jar,
        headers: {Accept: "application/json"},
        tags: {endpoint: "csrf"},
    });

    const csrfFromCookie = captureCsrfCookie(response, jar);

    let csrfFromBody = null;
    try {
        const parsed = JSON.parse(response.body || "{}");
        csrfFromBody = parsed?.data?.token || null;
    } catch (_) {
        // ignore
    }

    const token = csrfFromCookie || csrfFromBody;

    check(response, {
        "csrf: valid status": (r) => [200, 429].includes(r.status),
        "csrf: not server error": (r) => r.status < 500,
        "csrf: token present (cookie or body)": () => Boolean(token) || response.status !== 200,
    });

    return {response, token};
}

export function buildCsrfHeaders(csrfToken, extraHeaders = {}) {
    // Only include the header if we actually have a token.
    const headers = {...extraHeaders};
    if (csrfToken) {
        headers["X-XSRF-TOKEN"] = csrfToken;
    }
    return headers;
}

export function loginAndCaptureCookies(deviceId, jar) {
    const loginResponse = http.post(
        `${BASE_URL}/api/v1/auth/identifier/login`,
        JSON.stringify({
            identifier: EMAIL,
            password: PASSWORD,
            device_id: deviceId,
        }),
        {
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json",
            },
            jar,
            tags: {endpoint: "login"},
        },
    );

    const cookies = captureAuthCookies(loginResponse, jar);

    check(loginResponse, {
        "login returned a non-server-error status": (r) => r.status < 500,
        "login produced expected status": (r) => [200, 401, 429].includes(r.status),
    });

    return {
        response: loginResponse,
        ...cookies,
    };
}

export function buildDeviceId(suffix) {
    return `${DEVICE_ID_PREFIX}-${suffix}`;
}

