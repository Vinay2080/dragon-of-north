package org.miniProjectTwo.DragonOfNorth.config.OtpConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Configuration class for an Amazon Simple Notification Service (SNS) client.
 * This class provides the necessary configuration to create and configure
 * the AWS SNS client used for sending SMS and push notifications in the application.
 */

@Configuration
public class SnsConfig {

    @Value("${aws.region}")
    private String region;

    /**
     * Creates and configures a new instance of {@link SnsClient}.
     * The client is configured with the AWS region specified in the application properties.
     *
     * @return A configured instance of {@link SnsClient} ready for use
     */

    @Bean
    public SnsClient snsClient() {
        return SnsClient
                .builder()
                .region(Region.of(region))
                .build();
    }
}
