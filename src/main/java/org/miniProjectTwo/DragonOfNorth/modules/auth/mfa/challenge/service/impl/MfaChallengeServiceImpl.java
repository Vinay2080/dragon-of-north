package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.service.impl;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.config.MfaChallengeProperties;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.ChallengeState;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.MfaChallenge;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.redis.ChallengeStateRedisStore;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.service.MfaChallengeService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.token.MfaChallengeTokenGenerator;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.TokenHasher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MfaChallengeServiceImpl implements MfaChallengeService {
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private final MfaChallengeTokenGenerator tokenGenerator;
    private final ChallengeStateRedisStore store;
    private final MfaChallengeProperties properties;
    private final TokenHasher tokenHasher;
    private final AuditEventLogger auditEventLogger;

    @Override
    public MfaChallenge createChallenge(UUID userId,
                                        String primaryAmr,
                                        AuthRequestContext context,
                                        List<ProviderType> availableMethods) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (primaryAmr == null || primaryAmr.isBlank()) {
            throw new IllegalArgumentException("primaryAmr must not be blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        String token = tokenGenerator.generateToken();
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(properties.getTtl());

        ChallengeState state = new ChallengeState(
                userId,
                primaryAmr.trim(),
                normalizeNullable(context.deviceId()),
                ipPrefix(context.ipAddress()),
                userAgentHash(context.userAgent()),
                0,
                createdAt,
                expiresAt
        );

        store.save(token, state);
        auditEventLogger.log("auth.mfa.challenge.issued",
                userId,
                context.deviceId(),
                context.ipAddress(),
                "success",
                null,
                context.requestId());

        return new MfaChallenge(token, expiresAt, availableMethods);
    }

    @Override
    public Optional<ChallengeState> peek(String mfaToken) {
        if (mfaToken == null || mfaToken.isBlank()) {
            throw new IllegalArgumentException("mfaToken must not be blank");
        }
        return store.find(mfaToken.trim());
    }

    @Override
    public void invalidate(String mfaToken, AuthRequestContext context) {
        if (mfaToken == null || mfaToken.isBlank()) {
            throw new IllegalArgumentException("mfaToken must not be blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        Optional<ChallengeState> state = store.find(mfaToken.trim());
        boolean deleted = store.deleteIfPresent(mfaToken.trim());

        UUID userId = state.map(ChallengeState::userId).orElse(null);
        String reason = deleted ? null : "not_found";
        auditEventLogger.log("auth.mfa.challenge.invalidated",
                userId,
                context.deviceId(),
                context.ipAddress(),
                "success",
                reason,
                context.requestId());
    }

    private String userAgentHash(String rawUserAgent) {
        String normalized = normalizeNullable(rawUserAgent);
        if (normalized == null) {
            return null;
        }
        String truncated = normalized.length() > MAX_USER_AGENT_LENGTH
                ? normalized.substring(0, MAX_USER_AGENT_LENGTH)
                : normalized;
        return tokenHasher.hashToken(truncated);
    }

    private String ipPrefix(String rawIpAddress) {
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
            return String.join(":", java.util.Arrays.copyOf(parts, keep));
        }

        // IPv4: keep the first 3 octets.
        String[] parts = first.split("\\.", -1);
        if (parts.length < 3) {
            return first;
        }
        return parts[0] + "." + parts[1] + "." + parts[2];
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}

