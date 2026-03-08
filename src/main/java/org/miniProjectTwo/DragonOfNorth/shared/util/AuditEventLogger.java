package org.miniProjectTwo.DragonOfNorth.shared.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Centralized structured audit logger.
 *
 * <p>All auth/session/otp flows should log audit events using a consistent schema:
 * event, user_id, device_id, ip, result, reason, request_id.
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
        log.info("event={} user_id={} device_id={} ip={} result={} reason={} request_id={}",
                event,
                userId,
                deviceId,
                ip,
                result,
                reason,
                requestId);
    }
}
