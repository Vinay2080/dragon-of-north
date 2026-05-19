package org.miniProjectTwo.DragonOfNorth.infrastructure.encryption;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds externally supplied encryption settings.
 *
 * <p>The master key must be a Base64-encoded 32-byte AES key. It is intentionally
 * not defaulted because production keys must come from environment or secret
 * configuration.</p>
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
