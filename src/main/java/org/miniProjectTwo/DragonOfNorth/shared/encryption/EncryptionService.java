package org.miniProjectTwo.DragonOfNorth.shared.encryption;

/**
 * Application-level contract for encrypting sensitive values before storage.
 */
public interface EncryptionService {

    /**
     * Encrypts plaintext into a storage-safe encoded encrypted value.
     *
     * @param plainText sensitive value to encrypt
     * @return encoded encrypted value
     */
    String encrypt(String plainText);

    /**
     * Decrypts a value produced by {@link #encrypt(String)}.
     *
     * @param encryptedText encoded encrypted value
     * @return original plaintext
     */
    String decrypt(String encryptedText);
}
