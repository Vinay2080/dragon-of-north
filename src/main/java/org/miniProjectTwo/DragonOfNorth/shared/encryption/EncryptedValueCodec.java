package org.miniProjectTwo.DragonOfNorth.shared.encryption;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Serialization contract for encrypted envelope format exchanged between encryption layers/storage.
 */
@Component
public class EncryptedValueCodec {

    private final ObjectMapper objectMapper;

    public EncryptedValueCodec() {
        this.objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String serialize(EncryptedValue encryptedValue) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(encryptedValue);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (JsonProcessingException e) {
            throw new EncryptionException("Failed to serialize encrypted value", e);
        }
    }

    public EncryptedValue parse(String encodedValue) {
        if (encodedValue == null || encodedValue.isBlank()) {
            throw new EncryptionException("Encrypted value must not be blank");
        }

        try {
            byte[] json = Base64.getUrlDecoder().decode(encodedValue.trim());
            return objectMapper.readValue(new String(json, StandardCharsets.UTF_8), EncryptedValue.class);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            throw new EncryptionException("Failed to parse encrypted value", e);
        }
    }
}
