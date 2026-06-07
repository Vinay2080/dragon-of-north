package org.miniProjectTwo.DragonOfNorth.infrastructure.encryption;

import javax.crypto.SecretKey;

/**
 * Immutable key material bundle containing a stable key identifier and JCE secret key.
 * <p> The key identifier is a stable, opaque string that can be used to look up the correct decryption key for a given encrypted payload. The secret key is the actual cryptographic material used for encryption and decryption operations. By bundling these together, we can easily manage encryption keys and support future key rotation without breaking existing encrypted data.
 * <p>
 * The key identifier is embedded into encrypted payload metadata so decrypt can resolve the correct
 * key after future key rotation.
 */
record EncryptionKey(String keyId, SecretKey secretKey) {
}
