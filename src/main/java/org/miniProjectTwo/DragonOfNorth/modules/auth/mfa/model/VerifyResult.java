package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model;

import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

import java.time.Instant;

public record VerifyResult(
        boolean success,
        ProviderType providerType,
        Instant verifiedAt,
        String failureReason
) {
    public static VerifyResult success(ProviderType providerType) {
        return new VerifyResult(true, providerType, Instant.now(), null);
    }

    public static VerifyResult failure(ProviderType providerType, String failureReason) {
        return new VerifyResult(false, providerType, null, failureReason);
    }
}
