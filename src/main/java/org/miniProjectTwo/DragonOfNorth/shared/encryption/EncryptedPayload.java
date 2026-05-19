package org.miniProjectTwo.DragonOfNorth.shared.encryption;

/**
 * AES-GCM payload data. The ciphertext value contains encrypted bytes followed
 * by the GCM authentication tag as returned by {@code Cipher#doFinal}.
 */
public record EncryptedPayload(String iv, String ciphertextWithTag) {

    public EncryptedPayload {
        requireText(iv, "iv");
        iv = iv.trim();
        requireText(ciphertextWithTag, "ciphertextWithTag");
        ciphertextWithTag = ciphertextWithTag.trim();
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
