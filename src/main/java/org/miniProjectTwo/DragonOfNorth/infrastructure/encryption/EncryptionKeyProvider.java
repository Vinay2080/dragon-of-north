package org.miniProjectTwo.DragonOfNorth.infrastructure.encryption;

interface EncryptionKeyProvider {

    EncryptionKey currentKey();

    EncryptionKey keyFor(String keyId);
}
