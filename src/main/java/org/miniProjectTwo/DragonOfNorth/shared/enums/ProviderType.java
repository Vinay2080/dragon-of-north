package org.miniProjectTwo.DragonOfNorth.shared.enums;

/**
 * Supported MFA verification providers.
 *
 * <p>This enum is used for expressing which challenge methods are available for
 * a user during an MFA challenge, independent of how those methods are later
 * verified.</p>
 */
public enum ProviderType {
    TOTP,
    RECOVERY_CODE
}

