package org.miniProjectTwo.DragonOfNorth.infrastructure.encryption;

import javax.crypto.SecretKey;

/**
 * Immutable key material bundle containing a stable key identifier and JCE secret key.
 * <p>
 * The key identifier is embedded into encrypted payload metadata so decrypt can resolve the correct
 * key after future key rotation.
 */
record EncryptionKey(String keyId, SecretKey secretKey) {
}
