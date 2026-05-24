package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Transport-neutral request metadata for authentication/audit decisions.
 * <p>
 * This record keeps servlet concerns at the controller boundary while allowing services to consume
 * device identity, request tracing, and user-agent/ip context for risk checks and observability.
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

