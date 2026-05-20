package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.redis;

import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.ChallengeState;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeStateCodecTest {

    private final ChallengeStateCodec codec = new ChallengeStateCodec();

    @Test
    void serializeAndDeserialize_shouldRoundTrip() {
        ChallengeState state = new ChallengeState(
                UUID.randomUUID(),
                "pwd",
                "device-1",
                "10.0.0",
                "ua-hash",
                2,
                Instant.parse("2026-05-20T00:00:00Z"),
                Instant.parse("2026-05-20T00:05:00Z")
        );

        String encoded = codec.serialize(state);
        ChallengeState decoded = codec.deserialize(encoded);

        assertEquals(state, decoded);
    }

    @Test
    void deserialize_shouldRejectBlank() {
        assertThrows(IllegalArgumentException.class, () -> codec.deserialize(" "));
    }

    @Test
    void deserialize_shouldRejectUnsupportedVersion() {
        ChallengeState state = new ChallengeState(
                UUID.randomUUID(),
                "pwd",
                "device-1",
                "10.0.0",
                "ua-hash",
                0,
                Instant.parse("2026-05-20T00:00:00Z"),
                Instant.parse("2026-05-20T00:05:00Z")
        );

        String encoded = codec.serialize(state).replace("\"v\":1", "\"v\":2");
        assertThrows(IllegalArgumentException.class, () -> codec.deserialize(encoded));
    }
}

