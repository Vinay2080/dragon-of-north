package org.miniProjectTwo.DragonOfNorth.shared.encryption;

import java.util.Map;
import java.util.Objects;

/**
 * Versioned encrypted value envelope.
 *
 * <p>The envelope carries key metadata separately from the AES-GCM payload so
 * future key rotation can resolve historical keys by {@code keyId}.</p>
 */
public record EncryptedValue(
        int version,
        String keyId,
        EncryptedPayload payload,
        Map<String, String> attributes
) {

    public static final int CURRENT_VERSION = 1;

    public EncryptedValue(int version, String keyId, EncryptedPayload payload) {
        this(version, keyId, payload, Map.of());
    }

    public EncryptedValue {
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
        requireText(keyId);
        keyId = keyId.trim();
        Objects.requireNonNull(payload, "payload must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    private static void requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("keyId" + " must not be blank");
        }
    }
}
