package org.miniProjectTwo.DragonOfNorth.infrastructure.encryption;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.shared.encryption.EncryptionException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Property-backed key resolver used by encryption services in auth/security workflows.
 * <p>
 * Current implementation resolves only one active AES-256 key from {@link EncryptionProperties}.
 * It still enforces key-id lookup semantics so callers and payload formats remain compatible with
 * future multi-key rotation (KMS/Vault/key-ring tables). Failure behavior is fail-closed: unknown
 * key ids or invalid key material throw {@link org.miniProjectTwo.DragonOfNorth.shared.encryption.EncryptionException}.
 */
@Component
@RequiredArgsConstructor
class ConfiguredEncryptionKeyProvider implements EncryptionKeyProvider {

    private static final int AES_256_KEY_BYTES = 32;
    private static final String AES_ALGORITHM = "AES";

    private final EncryptionProperties properties;

    @Override
    public EncryptionKey currentKey() {
        return keyFor(activeKeyId());
    }

    @Override
    public EncryptionKey keyFor(String keyId) {
        String activeKeyId = activeKeyId();
        if (!activeKeyId.equals(normalizeKeyId(keyId))) {
            throw new EncryptionException("No AES encryption key configured for keyId: " + keyId);
        }
        return new EncryptionKey(activeKeyId, secretKey());
    }

    private SecretKey secretKey() {
        String encodedKey = properties.getMasterKey();
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new EncryptionException("app.encryption.master-key must be configured");
        }

        byte[] keyBytes = decodeBase64Key(encodedKey.trim());
        if (keyBytes.length != AES_256_KEY_BYTES) {
            throw new EncryptionException("app.encryption.master-key must decode to 32 bytes for AES-256");
        }
        return new SecretKeySpec(keyBytes, AES_ALGORITHM);
    }

    private byte[] decodeBase64Key(String encodedKey) {
        try {
            return Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException ignored) {
            try {
                return Base64.getUrlDecoder().decode(encodedKey);
            } catch (IllegalArgumentException e) {
                throw new EncryptionException("app.encryption.master-key must be Base64 encoded", e);
            }
        }
    }

    private String activeKeyId() {
        return normalizeKeyId(properties.getActiveKeyId());
    }

    private String normalizeKeyId(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            throw new EncryptionException("app.encryption.active-key-id must be configured");
        }
        return keyId.trim();
    }
}
