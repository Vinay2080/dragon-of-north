package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Transport-neutral request metadata for authentication/audit decisions.
 * <p>
 * This record keeps servlet concerns at the controller boundary while allowing services to consume
 * device identity, request tracing, and user-agent/ip context for risk checks and observability.
 * @param deviceId client-generated device identifier from the request body
 * @param ipAddress client IP address from the X-Forwarded-For header
 * @param requestId unique request identifier from the X-Request-Id header for tracing
 * @param userAgent client user agent string from the User-Agent header
 */
public record AuthRequestContext(
        String deviceId,
        String ipAddress,
        String requestId,
        String userAgent
) {
    /**
     * Builds context from incoming servlet headers and the client-provided device id.
     * <p>
     * If any required headers are missing, the deviceId is used as a fallback for ipAddress and userAgent. This allows the context to be constructed even in less common environments (e.g., mobile clients without standard headers) while still providing some traceability.
     * @param request incoming HTTP servlet request
     * @param deviceId client-generated device identifier from the request body
     * @return constructed AuthRequestContext with extracted header values and device id
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

