package org.miniProjectTwo.DragonOfNorth.shared.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuditEventLoggerTest {

    @InjectMocks
    private AuditEventLogger auditEventLogger;

    @Test
    void log_shouldNotThrow_whenAllParametersProvided() {
        assertDoesNotThrow(() -> auditEventLogger.log(
                SecurityAuditEvent.AUTH_LOGIN_SUCCESS,
                UUID.randomUUID(),
                "device-123",
                "192.168.0.1",
                "success",
                "ok",
                UUID.randomUUID().toString()
        ));
    }

    @Test
    void sanitize_shouldRedactSensitiveArtifacts() {
        String raw = "challenge=abc token=xyz code=123456 Bearer eyJhbGciOiJIUzI1NiJ9";
        String redacted = AuditEventLogger.sanitize(raw);

        assertFalse(redacted.contains("abc"));
        assertFalse(redacted.contains("xyz"));
        assertFalse(redacted.contains("123456"));
        assertFalse(redacted.contains("eyJhbGciOiJIUzI1NiJ9"));
        assertTrue(redacted.contains("[REDACTED]"));
    }

    @Test
    void securityAuditContext_shouldExposeStructuredFields() {
        SecurityAuditContext context = new SecurityAuditContext(
                UUID.randomUUID(), UUID.randomUUID(), "device-1", "req-1", "203.0.113.1",
                "ua-hash", "pwd", "totp", "failure", "context_mismatch", "challenge_ref=aa11"
        );

        assertEquals("device-1", context.toFields().get("device_id"));
        assertEquals("ua-hash", context.toFields().get("user_agent_hash"));
        assertEquals("challenge_ref=aa11", context.toFields().get("challenge_ref"));
        assertNotNull(context.toFields().get("ts"));
    }
}
