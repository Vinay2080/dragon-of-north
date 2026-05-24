package org.miniProjectTwo.DragonOfNorth.shared.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
