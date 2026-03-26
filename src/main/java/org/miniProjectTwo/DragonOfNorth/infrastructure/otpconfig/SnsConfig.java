package org.miniProjectTwo.DragonOfNorth.infrastructure.otpconfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.NamedInterface;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Creates the AWS SNS client used by SMS delivery components.
 */
@NamedInterface
@Configuration
public class SnsConfig {

    /**
     * AWS region loaded from {@code aws.region}.
     */
    @Value("${aws.region}")
    private String region;

    /**
     * Builds the SNS client for the configured region.
     *
     * @return configured SNS client
     */
    @Bean
    public SnsClient snsClient() {
        return SnsClient
                .builder()
                .region(Region.of(region))
                .build();
    }
}
//todo easiest ways to send sms