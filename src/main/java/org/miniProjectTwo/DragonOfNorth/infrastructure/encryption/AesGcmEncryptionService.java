package org.miniProjectTwo.DragonOfNorth.infrastructure.encryption;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.shared.encryption.*;
import org.springframework.stereotype.Service;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Authenticated encryption component used by security-sensitive modules (for example MFA secret
 * storage and other encrypted-at-rest values).
 * <p>
 * Dependency workflow: callers provide plaintext -> provider resolves key -> AES-GCM encrypts with
 * random IV and AAD(version:keyId) -> codec serializes envelope. Decrypt reverses this using key id
 * from envelope metadata, ensuring tamper detection through GCM authentication tag.
 * <p>
 * Input guarantees: plaintext must be non-null; encrypted text must conform to codec format.
 * Failure behavior: malformed payloads, unknown key ids, invalid tags, or crypto failures throw
 * {@link org.miniProjectTwo.DragonOfNorth.shared.encryption.EncryptionException}. The service is
 * stateless and thread-safe for concurrent request handling.
 */
@Service
@RequiredArgsConstructor
class AesGcmEncryptionService implements EncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final EncryptionKeyProvider keyProvider;
    private final EncryptedValueCodec encryptedValueCodec;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String encrypt(String plainText) {
        if (plainText == null) {
            throw new EncryptionException("Plaintext must not be null");
        }

        try {
            EncryptionKey encryptionKey = keyProvider.currentKey();
            byte[] iv = randomIv();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey.secretKey(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad(EncryptedValue.CURRENT_VERSION, encryptionKey.keyId()));

            byte[] ciphertextWithTag = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            EncryptedValue encryptedValue = new EncryptedValue(
                    EncryptedValue.CURRENT_VERSION,
                    encryptionKey.keyId(),
                    new EncryptedPayload(base64(iv), base64(ciphertextWithTag))
            );
            return encryptedValueCodec.serialize(encryptedValue);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Failed to encrypt value", e);
        }
    }

    @Override
    public String decrypt(String encryptedText) {
        EncryptedValue encryptedValue = encryptedValueCodec.parse(encryptedText);
        if (encryptedValue.version() != EncryptedValue.CURRENT_VERSION) {
            throw new EncryptionException("Unsupported encrypted value version: " + encryptedValue.version());
        }

        try {
            EncryptionKey encryptionKey = keyProvider.keyFor(encryptedValue.keyId());
            byte[] iv = decodeBase64(encryptedValue.payload().iv(), "iv");
            byte[] ciphertextWithTag = decodeBase64(encryptedValue.payload().ciphertextWithTag(), "ciphertextWithTag");

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey.secretKey(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad(encryptedValue.version(), encryptedValue.keyId()));

            byte[] plainText = cipher.doFinal(ciphertextWithTag);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw new EncryptionException("Encrypted value authentication failed", e);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Failed to decrypt value", e);
        }
    }

    private byte[] decodeBase64(String value, String fieldName) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new EncryptionException("Encrypted payload " + fieldName + " is not valid Base64", e);
        }
    }

    private byte[] randomIv() {
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    private byte[] aad(int version, String keyId) {
        return (version + ":" + keyId).getBytes(StandardCharsets.UTF_8);
    }

    private String base64(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }
}
