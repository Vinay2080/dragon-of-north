package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.provider;

public record ChallengeProviderVerification(boolean success, String failureReason) {
    public static ChallengeProviderVerification verified() {
        return new ChallengeProviderVerification(true, null);
    }

    public static ChallengeProviderVerification failure(String reason) {
        return new ChallengeProviderVerification(false, reason);
    }
}
