package org.miniProjectTwo.DragonOfNorth.config.OtpConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * Configuration class for Amazon Simple Email Service (SES) client.
 * This class provides the necessary configuration to create and configure
 * the AWS SES client used for sending emails in the application.
 */

@Configuration

public class SesConfig {

    @Value("${aws.region}")
    private String region;

    /**
     * Creates and configures a new instance of {@link SesClient}.
     * The client is configured with the AWS region specified in the application properties.
     *
     * @return A configured instance of {@link SesClient} ready for use
     */

    @Bean
    public SesClient sesClient() {
        return SesClient
                .builder()
                .region(Region.of(region))
                .build();
    }

}