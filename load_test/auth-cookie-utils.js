import http from "k6/http";
import {check} from "k6";

export const BASE_URL = __ENV.BASE_URL || "https://dragon-api.duckdns.org";
export const EMAIL = __ENV.EMAIL;
export const PASSWORD = __ENV.PASSWORD;

export function requireCredentials() {
    if (!EMAIL || !PASSWORD) {
        throw new Error("EMAIL and PASSWORD environment variables are required.");
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
            secure: true,
            http_only: true,
        });
    }

    if (refreshToken) {
        jar.set(BASE_URL, "refresh_token", refreshToken, {
            path: "/api/v1/auth/jwt/refresh",
            secure: true,
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
            secure: true,
            http_only: true,
        });
    }

    if (manualRefreshCookie) {
        jar.set(BASE_URL, "refresh_token", manualRefreshCookie, {
            path: "/api/v1/auth/jwt/refresh",
            secure: true,
            http_only: true,
        });
    }

    return {
        hasManualAccessCookie: Boolean(manualAccessCookie),
        hasManualRefreshCookie: Boolean(manualRefreshCookie),
    };
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
