package org.miniProjectTwo.DragonOfNorth.infrastructure.encryption;

/**
 * Abstraction for key resolution in encryption/decryption lifecycles.
 * <p>
 * Why this exists: encryption callers (auth services, MFA components, future token stores) should
 * not know where keys come from (properties, KMS, HSM, secret manager, key service).
 * <ul>
 *   <li>{@link #currentKey()} supplies key material for new writes.</li>
 *   <li>{@link #keyFor(String)} must support decrypt of historical payloads by key id.</li>
 * </ul>
 * Implementations should be thread-safe, deterministic, and fail-closed on unknown ids or invalid
 * key state. Breaking these guarantees can corrupt decrypt paths across modules.
 */
interface EncryptionKeyProvider {
    /**
     * Retrieves the current encryption key for new writes.
     *
     * @return The current encryption key.
     */
    EncryptionKey currentKey();

    /**
     * Retrieves the encryption key for the specified key id.
     *
     * @param keyId The unique identifier for the encryption key.
     * @return The encryption key for the specified key id.
     */
    EncryptionKey keyFor(String keyId);
}
