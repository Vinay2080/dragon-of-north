package org.miniProjectTwo.DragonOfNorth.shared.util;

import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

/**
 * Central identifier normalization utility for consistent email/phone canonicalization.
 * Divergent normalization can break resolver routing, lookup consistency, and rate-limit keys.
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
