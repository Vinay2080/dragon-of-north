package org.miniProjectTwo.DragonOfNorth.shared.util;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record SecurityAuditContext(
        UUID userId,
        UUID sessionId,
        String deviceId,
        String requestId,
        String ip,
        String userAgentHash,
        String authMethod,
        String providerType,
        String result,
        String reason,
        String challengeRef
) {
    public Map<String, Object> toFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("user_id", userId);
        fields.put("session_id", sessionId);
        fields.put("device_id", deviceId);
        fields.put("request_id", requestId);
        fields.put("ip", ip);
        fields.put("user_agent_hash", userAgentHash);
        fields.put("auth_method", authMethod);
        fields.put("provider_type", providerType);
        fields.put("result", result);
        fields.put("reason", reason);
        fields.put("challenge_ref", challengeRef);
        fields.put("ts", Instant.now().toString());
        return fields;
    }
}
