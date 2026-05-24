package org.miniProjectTwo.DragonOfNorth.infrastructure.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Central Cloudinary client factory used by profile image flows.
 * <p>
 * Primary consumer: profile services/controllers that upload avatar assets. Keeping creation here
 * avoids leaking provider credentials into business modules and allows future migration to secret
 * managers, per-tenant Cloudinary accounts, or alternative media backends behind the same bean.
 * Invalid/missing properties fail application startup, which is intentional for fail-fast ops.
 */
@Configuration
public class CloudinaryConfig {

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
