package org.miniProjectTwo.DragonOfNorth.security.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuthnFacts(
        UUID userId,
        List<String> roles,
        boolean mfaVerified,
        Instant mfaVerifiedAt,
        List<String> amr,
        UUID sessionId
) {
}

