package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Request metadata used by auth services without leaking servlet APIs into service logic.
 */
public record AuthRequestContext(
        String deviceId,
        String ipAddress,
        String requestId,
        String userAgent
) {
    /**
     * Builds context from incoming servlet headers and the client-provided device id.
     */
    public static AuthRequestContext fromHttpRequest(HttpServletRequest request, String deviceId) {
        return new AuthRequestContext(
                deviceId,
                request.getHeader("X-Forwarded-For"),
                request.getHeader("X-Request-Id"),
                request.getHeader("User-Agent")
        );
    }
}

