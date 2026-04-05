package org.miniProjectTwo.DragonOfNorth.shared.util;

import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

/**
 * Utility for consistent identifier normalization across auth and OTP flows.
 */
public final class IdentifierNormalizer {

    private IdentifierNormalizer() {
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public static String normalizePhone(String phone) {
        return phone == null ? null : phone.replace(" ", "");
    }

    public static String normalize(String identifier, IdentifierType identifierType) {
        return identifierType == IdentifierType.EMAIL
                ? normalizeEmail(identifier)
                : normalizePhone(identifier);
    }
}
