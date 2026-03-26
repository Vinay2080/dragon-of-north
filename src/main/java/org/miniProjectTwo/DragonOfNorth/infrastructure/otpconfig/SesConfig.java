package org.miniProjectTwo.DragonOfNorth.infrastructure.otpconfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * Creates the AWS SES client used by email delivery components.
 */
@Configuration
public class SesConfig {

    /**
     * AWS region loaded from {@code aws.region}.
     */
    @Value("${aws.region}")
    private String region;

    /**
     * Builds the SES client for the configured region.
     *
     * @return configured SES client
     */
    @Bean
    public SesClient sesClient() {
        return SesClient
                .builder()
                .region(Region.of(region))
                .build();
    }

}