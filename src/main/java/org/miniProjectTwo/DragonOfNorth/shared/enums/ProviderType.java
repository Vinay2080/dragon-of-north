package org.miniProjectTwo.DragonOfNorth.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Supported MFA verification providers.
 *
 * <p>Stable provider keys make future provider additions safe for serialization,
 * persistence, and challenge orchestration.</p>
 */
@Getter
@RequiredArgsConstructor
public enum ProviderType {
    TOTP("totp"),
    RECOVERY_CODE("recovery_code");

    private final String key;
}
