package org.miniProjectTwo.DragonOfNorth.modules.session.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model returned to clients for session-management screens.
 */
@Schema(name = "SessionSummaryResponse", description = "Summary view of one device session belonging to the authenticated user.")
public record SessionSummaryResponse(
        @Schema(description = "Unique session identifier.", example = "a987ab67-14b7-4f1e-b77f-cfd61133cc3b")
        UUID sessionId,
        @Schema(description = "Client-generated device identifier associated with the session.", example = "web-chrome-macos")
        String deviceId,
        @Schema(description = "Most recent client IP address seen for the session.", example = "203.0.113.10", nullable = true)
        String ipAddress,
        @Schema(description = "Most recent User-Agent header seen for the session.", example = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_3)", nullable = true)
        String userAgent,
        @Schema(description = "Timestamp when the session was last used.", example = "2026-04-04T06:30:00Z")
        Instant lastUsedAt,
        @Schema(description = "Timestamp when the session expires.", example = "2026-04-11T06:30:00Z")
        Instant expiryDate,
        @Schema(description = "Whether the session has been revoked.", example = "false")
        boolean revoked
) {
}
