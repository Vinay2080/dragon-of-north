package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.token;

/**
 * Generates opaque MFA challenge tokens.
 *
 * <p>Tokens are not JWTs. They are random, high-entropy identifiers used to look
 * up mutable challenge state in Redis.</p>
 */
public interface MfaChallengeTokenGenerator {
    String generateToken();
}

