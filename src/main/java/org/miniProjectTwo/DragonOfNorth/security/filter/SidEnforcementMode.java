package org.miniProjectTwo.DragonOfNorth.security.filter;

import java.util.Locale;

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
