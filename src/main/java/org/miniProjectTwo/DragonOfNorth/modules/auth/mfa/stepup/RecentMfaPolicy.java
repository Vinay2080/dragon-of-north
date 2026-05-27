package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup;

/**
 * Policy identifiers for per-operation recent-MFA freshness windows.
 */
public enum RecentMfaPolicy {
    DEFAULT,
    PASSWORD_CHANGE,
    ACCOUNT_DELETE,
    RECOVERY_CODE_REGENERATION,
    SESSION_REVOKE,
    SESSION_REVOKE_ALL
}
