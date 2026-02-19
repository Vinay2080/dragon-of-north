package org.miniProjectTwo.DragonOfNorth.dto.session.response;

import java.time.Instant;
import java.util.UUID;

public record SessionSummaryResponse(
        UUID sessionId,
        String deviceId,
        String ipAddress,
        String userAgent,
        Instant lastUsedAt,
        Instant expiryDate,
        boolean revoked
) {
}
