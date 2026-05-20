package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.redis;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MfaChallengeRedisKeysTest {

    @Test
    void challengeKey_shouldUseStandardPrefixAndTrim() {
        assertEquals("auth:mfa:challenge:token-1", MfaChallengeRedisKeys.challengeKey(" token-1 "));
    }

    @Test
    void challengeKey_shouldRejectBlank() {
        assertThrows(IllegalArgumentException.class, () -> MfaChallengeRedisKeys.challengeKey(" "));
    }

    @Test
    void lockoutKey_shouldUseStandardPrefix() {
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        assertEquals("auth:mfa:lockout:" + userId, MfaChallengeRedisKeys.lockoutKey(userId));
    }
}

