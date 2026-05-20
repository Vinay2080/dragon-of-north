package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model;

import java.util.Map;
import java.util.UUID;

public record MfaContext(
        boolean stepUp,
        UUID sessionId,
        String ipAddress,
        String userAgent,
        String deviceId,
        Map<String, Object> attributes
) {
    public static MfaContext empty() {
        return new MfaContext(false, null, null, null, null, Map.of());
    }
}
