package org.miniProjectTwo.DragonOfNorth.security.filter;

import java.util.Locale;

/**
 * Enum representing the SID enforcement modes for session management.
 *
 * <p>The SID enforcement mode determines how the application enforces session integrity
 * and multifactor authentication (MFA) requirements. The modes are:</p>
 *
 * <ul>
 *     <li>{@code DISABLED}: No SID enforcement is applied. All authenticated sessions are allowed.</li>
 *     <li>{@code SENSITIVE_ONLY}: SID enforcement is applied only to sensitive operations or endpoints.</li>
 *     <li>{@code MFA_ONLY}: SID enforcement is applied only to sessions that have completed MFA.</li>
 *     <li>{@code ALL_AUTHENTICATED}: SID enforcement is applied to all authenticated sessions, regardless of MFA status.</li>
 * </ul>
 */
public enum SidEnforcementMode {
    DISABLED,
    SENSITIVE_ONLY,
    MFA_ONLY,
    ALL_AUTHENTICATED;

    public static SidEnforcementMode from(String value) {
        if (value == null || value.isBlank()) {
            return DISABLED;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return SidEnforcementMode.valueOf(normalized);
    }
}
