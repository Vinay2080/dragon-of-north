package org.miniProjectTwo.DragonOfNorth.config.OtpConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * Configuration class for an Amazon Simple Email Service (SES) client.
 * <p>
 * Provides the necessary configuration to create and configure the AWS SES client
 * used for sending OTP emails and other email communications in the authentication system.
 * Reads an AWS region from application properties for proper service endpoint configuration.
 */
@Configuration
public class SesConfig {

    /**
     * AWS region for SES service endpoint.
     * Configured via {@code aws.region} property in application properties.
     */
    @Value("${aws.region}")
    private String region;

    /**
     * Creates and configures a new instance of {@link SesClient}.
     * <p>
     * The client is configured with the AWS region specified in the application properties
     * and uses a default credential provider chain for authentication.
     * This bean is used by email services for sending OTP verification emails.
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