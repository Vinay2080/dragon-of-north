package org.miniProjectTwo.DragonOfNorth.dto.auth.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Session summary payload returned to authenticated users.
 */
public record SessionSummaryResponse(
        UUID sessionId,
        String deviceId,
        String ipAddress,
        String userAgent,
        Instant lastUsedAt,
        Instant expiryDate,
        boolean revoked,
        boolean currentSession
) {
}
