package org.miniProjectTwo.DragonOfNorth.infrastructure.encryption;

import javax.crypto.SecretKey;

record EncryptionKey(String keyId, SecretKey secretKey) {
}
