package org.miniProjectTwo.DragonOfNorth.infrastructure.otpconfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * SES client factory used by OTP/email delivery services.
 * <p>
 * Region is sourced from configuration and resolved at startup. Misconfiguration causes fail-fast
 * bean creation failure, surfacing environment issues before handling authentication traffic.
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