package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.token;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Secure token generator for MFA challenges.
 *
 * <p>Uses 256 bits of entropy and Base64URL encoding without padding.</p>
 */
@Component
public class SecureMfaChallengeTokenGenerator implements MfaChallengeTokenGenerator {
    private static final int TOKEN_BYTES = 32; // 256-bit minimum entropy

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

