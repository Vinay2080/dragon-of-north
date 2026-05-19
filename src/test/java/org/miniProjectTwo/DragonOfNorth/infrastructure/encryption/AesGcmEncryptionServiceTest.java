package org.miniProjectTwo.DragonOfNorth.infrastructure.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.shared.encryption.EncryptedPayload;
import org.miniProjectTwo.DragonOfNorth.shared.encryption.EncryptedValue;
import org.miniProjectTwo.DragonOfNorth.shared.encryption.EncryptedValueCodec;
import org.miniProjectTwo.DragonOfNorth.shared.encryption.EncryptionException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmEncryptionServiceTest {

    private AesGcmEncryptionService encryptionService;
    private EncryptedValueCodec encryptedValueCodec;

    @BeforeEach
    void setUp() {
        EncryptionProperties properties = new EncryptionProperties();
        properties.setActiveKeyId("primary");
        properties.setMasterKey(Base64.getEncoder()
                .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));

        encryptedValueCodec = new EncryptedValueCodec();
        encryptionService = new AesGcmEncryptionService(
                new ConfiguredEncryptionKeyProvider(properties),
                encryptedValueCodec
        );
    }

    @Test
    void encryptAndDecryptRoundTrip() {
        String encrypted = encryptionService.encrypt("totp-secret-value");

        assertThat(encrypted).isNotEqualTo("totp-secret-value");
        assertThat(encryptionService.decrypt(encrypted)).isEqualTo("totp-secret-value");

        EncryptedValue encryptedValue = encryptedValueCodec.parse(encrypted);
        assertThat(encryptedValue.version()).isEqualTo(EncryptedValue.CURRENT_VERSION);
        assertThat(encryptedValue.keyId()).isEqualTo("primary");
        assertThat(encryptedValue.payload().iv()).isNotBlank();
        assertThat(encryptedValue.payload().ciphertextWithTag()).isNotBlank();
    }

    @Test
    void encryptUsesRandomIvForSamePlaintext() {
        String first = encryptionService.encrypt("same-secret");
        String second = encryptionService.encrypt("same-secret");

        assertThat(first).isNotEqualTo(second);
        assertThat(encryptionService.decrypt(first)).isEqualTo("same-secret");
        assertThat(encryptionService.decrypt(second)).isEqualTo("same-secret");
    }

    @Test
    void decryptRejectsTamperedCipherText() {
        String encrypted = encryptionService.encrypt("sensitive-secret");
        EncryptedValue encryptedValue = encryptedValueCodec.parse(encrypted);

        byte[] ciphertext = Base64.getDecoder().decode(encryptedValue.payload().ciphertextWithTag());
        ciphertext[0] = (byte) (ciphertext[0] ^ 1);

        EncryptedValue tampered = new EncryptedValue(
                encryptedValue.version(),
                encryptedValue.keyId(),
                new EncryptedPayload(
                        encryptedValue.payload().iv(),
                        Base64.getEncoder().encodeToString(ciphertext)
                )
        );

        assertThatThrownBy(() -> encryptionService.decrypt(encryptedValueCodec.serialize(tampered)))
                .isInstanceOf(EncryptionException.class)
                .hasMessageContaining("authentication failed");
    }
}
