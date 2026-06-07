package org.miniProjectTwo.DragonOfNorth.infrastructure.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Central Cloudinary client factory used by profile image flows.
 */
@Configuration
public class CloudinaryConfig {

    /**
     * Creates a Cloudinary client configured with the provided credentials.
     *
     * @param cloudName Cloudinary cloud name (from application properties)
     * @param apiKey    Cloudinary API key (from application properties)
     * @param apiSecret Cloudinary API secret (from application properties)
     * @return Configured Cloudinary client instance
     */
    @Bean
    public Cloudinary cloudinary(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
    ) {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }
}
