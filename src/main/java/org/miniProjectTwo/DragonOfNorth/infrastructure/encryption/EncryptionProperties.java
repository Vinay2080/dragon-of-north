package org.miniProjectTwo.DragonOfNorth.infrastructure.encryption;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized encryption settings consumed by {@link ConfiguredEncryptionKeyProvider}.
 * <p>
 * These values define active key identity and raw key material source. They are expected to come
 * from environment-backed configuration or secret managers in production. Misconfiguration blocks
 * encryption/decryption and can impact authentication/MFA flows that persist encrypted secrets.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.encryption")
public class EncryptionProperties {

    /**
     * Identifier for the active encryption key.
     */
    private String activeKeyId;

    /**
     * Base64-encoded 256-bit AES key.
     */
    private String masterKey;
}
