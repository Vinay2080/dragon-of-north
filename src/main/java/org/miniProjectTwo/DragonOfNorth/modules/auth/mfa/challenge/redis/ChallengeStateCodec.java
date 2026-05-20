package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.ChallengeState;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Encodes/decodes {@link ChallengeState} for Redis storage.
 *
 * <p>Uses an explicit storage payload structure (versioned) to avoid fragile
 * generic Java object serialization.</p>
 */
@Component
public class ChallengeStateCodec {
    private static final int VERSION = 1;

    private final ObjectMapper objectMapper;

    public ChallengeStateCodec() {
        this.objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String serialize(ChallengeState state) {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }

        ChallengeStatePayload payload = new ChallengeStatePayload(
                VERSION,
                state.userId() == null ? null : state.userId().toString(),
                state.primaryAmr(),
                state.deviceId(),
                state.ipPrefix(),
                state.userAgentHash(),
                state.attempts(),
                state.createdAt(),
                state.expiresAt()
        );

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize ChallengeState", e);
        }
    }

    public ChallengeState deserialize(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalArgumentException("encoded state must not be blank");
        }

        ChallengeStatePayload payload;
        try {
            payload = objectMapper.readValue(encoded.trim(), ChallengeStatePayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse ChallengeState payload", e);
        }

        if (payload.v() != VERSION) {
            throw new IllegalArgumentException("Unsupported ChallengeState payload version: " + payload.v());
        }

        UUID userId = payload.userId() == null ? null : UUID.fromString(payload.userId());
        return new ChallengeState(
                userId,
                payload.primaryAmr(),
                payload.deviceId(),
                payload.ipPrefix(),
                payload.userAgentHash(),
                payload.attempts(),
                payload.createdAt(),
                payload.expiresAt()
        );
    }

    /**
     * Versioned Redis storage payload for {@link ChallengeState}.
     */
    private record ChallengeStatePayload(
            int v,
            String userId,
            String primaryAmr,
            String deviceId,
            String ipPrefix,
            String userAgentHash,
            int attempts,
            Instant createdAt,
            Instant expiresAt
    ) {
    }
}

