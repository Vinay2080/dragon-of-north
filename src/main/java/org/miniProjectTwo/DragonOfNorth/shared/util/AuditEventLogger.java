package org.miniProjectTwo.DragonOfNorth.shared.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Shared audit logging facade used by auth/security flows for structured security event trails.
 */
@Component
@Slf4j
public class AuditEventLogger {

    public void log(String event,
                    UUID userId,
                    String deviceId,
                    String ip,
                    String result,
                    String reason,
                    String requestId) {
        logSecurity(event, new SecurityAuditContext(
                userId,
                null,
                deviceId,
                requestId,
                ip,
                null,
                null,
                null,
                result,
                sanitize(reason),
                null
        ));
    }

    public void logSecurity(String event, SecurityAuditContext context) {
        Map<String, Object> fields = context == null ? Map.of() : context.toFields();
        String payload = fields.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + sanitize(entry.getValue() == null ? null : entry.getValue().toString()))
                .collect(Collectors.joining(" "));
        log.info("event={} {}", event, payload);
    }

    static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = value
                .replaceAll("(?i)bearer\\s+[A-Za-z0-9._-]+", "[REDACTED_BEARER]")
                .replaceAll("(?i)(challenge[_-]?id|challenge|token|jwt|code|secret|lock[_-]?value)=([^\\s,;]+)", "$1=[REDACTED]");
        if (sanitized.length() > 256) {
            return sanitized.substring(0, 256);
        }
        return sanitized;
    }
}
