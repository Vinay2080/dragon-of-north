package org.miniProjectTwo.DragonOfNorth.shared.encryption;

/**
 * AES-GCM payload data. The ciphertext value contains encrypted bytes followed
 * by the GCM authentication tag as returned by {@code Cipher#doFinal}.
 */
public record EncryptedPayload(String iv, String ciphertextWithTag) {

    /**
     * Constructs an EncryptedPayload with trimmed iv and ciphertextWithTag.
     *
     * @param iv                The initialization vector for AES-GCM encryption.
     * @param ciphertextWithTag The Base64-encoded ciphertext with GCM authentication tag.
     * @throws IllegalArgumentException if iv or ciphertextWithTag is blank.
     */
    public EncryptedPayload {
        requireText(iv, "iv");
        iv = iv.trim();
        requireText(ciphertextWithTag, "ciphertextWithTag");
        ciphertextWithTag = ciphertextWithTag.trim();
    }

    /**
     * Validates that a string value is not null or blank.
     *
     * @param value     The string value to validate.
     * @param fieldName The name of the field being validated.
     * @throws IllegalArgumentException if the value is null or blank.
     */
    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
