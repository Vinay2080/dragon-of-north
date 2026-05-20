package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.context;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.shared.util.TokenHasher;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Produces stable, privacy-preserving request bindings for MFA challenges.
 */
@Component
@RequiredArgsConstructor
public class ChallengeRequestBinding {
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private final TokenHasher tokenHasher;

    public String normalizeDeviceId(String deviceId) {
        return normalizeNullable(deviceId);
    }

    public String ipPrefix(String rawIpAddress) {
        String ip = normalizeNullable(rawIpAddress);
        if (ip == null) {
            return null;
        }

        // Handle X-Forwarded-For with multiple values.
        String first = ip.split(",", 2)[0].trim();
        if (first.isBlank()) {
            return null;
        }

        if (first.contains(":")) {
            // IPv6-ish: keep the first 4 hextets (coarse prefix).
            String[] parts = first.split(":", -1);
            int keep = Math.min(4, parts.length);
            return String.join(":", Arrays.copyOf(parts, keep));
        }

        // IPv4: keep the first 3 octets.
        String[] parts = first.split("\\.", -1);
        if (parts.length < 3) {
            return first;
        }
        return parts[0] + "." + parts[1] + "." + parts[2];
    }

    public String userAgentHash(String rawUserAgent) {
        String normalized = normalizeNullable(rawUserAgent);
        if (normalized == null) {
            return null;
        }
        String truncated = normalized.length() > MAX_USER_AGENT_LENGTH
                ? normalized.substring(0, MAX_USER_AGENT_LENGTH)
                : normalized;
        return tokenHasher.hashToken(truncated);
    }

    public Bindings fromContext(AuthRequestContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        return new Bindings(
                normalizeDeviceId(context.deviceId()),
                ipPrefix(context.ipAddress()),
                userAgentHash(context.userAgent())
        );
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public record Bindings(String deviceId, String ipPrefix, String userAgentHash) {
    }
}

