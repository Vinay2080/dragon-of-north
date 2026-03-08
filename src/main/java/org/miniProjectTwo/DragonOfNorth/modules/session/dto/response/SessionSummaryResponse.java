package org.miniProjectTwo.DragonOfNorth.modules.session.dto.response;

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
