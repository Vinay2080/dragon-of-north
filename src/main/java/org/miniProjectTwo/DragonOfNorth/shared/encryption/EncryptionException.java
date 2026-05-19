package org.miniProjectTwo.DragonOfNorth.shared.encryption;

/**
 * Raised when encryption configuration, serialization, or cryptographic
 * authentication fails.
 */
public class EncryptionException extends RuntimeException {

    public EncryptionException(String message) {
        super(message);
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
