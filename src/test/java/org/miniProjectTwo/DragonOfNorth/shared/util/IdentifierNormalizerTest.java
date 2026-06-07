package org.miniProjectTwo.DragonOfNorth.shared.util;

import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IdentifierNormalizerTest {

    @Test
    void normalizeEmail_shouldLowercaseAndTrim() {
        assertEquals("user@example.com", IdentifierNormalizer.normalizeEmail("  USER@EXAMPLE.COM  "));
    }

    @Test
    void normalizeEmail_shouldReturnNull_whenInputIsNull() {
        assertNull(IdentifierNormalizer.normalizeEmail(null));
    }

    @Test
    void normalizeEmail_shouldPreserveAlreadyNormalized() {
        assertEquals("test@mail.com", IdentifierNormalizer.normalizeEmail("test@mail.com"));
    }

    @Test
    void normalizePhone_shouldRemoveSpaces() {
        assertEquals("+12345678901", IdentifierNormalizer.normalizePhone("+1 234 567 8901"));
    }

    @Test
    void normalizePhone_shouldReturnNull_whenInputIsNull() {
        assertNull(null);
    }

    @Test
    void normalizePhone_shouldPreserveNonSpaceCharacters() {
        assertEquals("+1234567890", IdentifierNormalizer.normalizePhone("+1234567890"));
    }

    @Test
    void normalize_shouldUseEmailNormalization_forEmailType() {
        assertEquals("hello@example.com", IdentifierNormalizer.normalize("  HELLO@EXAMPLE.COM  ", IdentifierType.EMAIL));
    }

    @Test
    void normalize_shouldUsePhoneNormalization_forPhoneType() {
        assertEquals("+447911123456", IdentifierNormalizer.normalize("+44 7911 123456", IdentifierType.PHONE));
    }
}
