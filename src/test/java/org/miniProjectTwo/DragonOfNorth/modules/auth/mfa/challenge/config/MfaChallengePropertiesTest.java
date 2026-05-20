package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MfaChallengePropertiesTest {

    @Test
    void defaults_shouldMatchExpectedTtlsAndLimits() {
        MfaChallengeProperties properties = new MfaChallengeProperties();

        assertEquals(Duration.ofMinutes(5), properties.getTtl());
        assertEquals(5, properties.getMaxAttempts());
        assertEquals(Duration.ofMinutes(15), properties.getLockoutTtl());
    }
}

