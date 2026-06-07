package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.provider;

import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

import java.util.UUID;

/**
 * Verifies MFA challenge codes for a given user and provider type.
 * <p>
 * This interface abstracts the verification logic for different MFA providers (e.g., TOTP, SMS, email) and allows the MFA service to delegate code verification to the appropriate provider implementation based on the provider type. Implementations of this interface should handle the specific verification logic for their respective MFA method, such as validating TOTP codes against a shared secret or checking SMS/email codes against stored values.
 */
public interface MfaChallengeProviderVerifier {
    ChallengeProviderVerification verify(UUID userId, ProviderType providerType, String code);
}
