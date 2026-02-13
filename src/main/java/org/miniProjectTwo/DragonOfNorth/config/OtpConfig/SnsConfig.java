package org.miniProjectTwo.DragonOfNorth.config.OtpConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.NamedInterface;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Configuration class for an Amazon Simple Notification Service (SNS) client.
 * Provides the necessary configuration to create and configure the AWS SNS client
 * used for sending OTP SMS messages and other SMS notifications in the authentication system.
 * Reads an AWS region from application properties for proper service endpoint configuration.
 */
@NamedInterface
@Configuration
public class SnsConfig {

    /**
     * AWS region for SNS service endpoint.
     * Configured via {@code aws.region} property in application properties.
     */
    @Value("${aws.region}")
    private String region;

    /**
     * Creates and configures a new instance of {@link SnsClient}.
     * The client is configured with the AWS region specified in the application properties
     * and uses a default credential provider chain for authentication.
     * This bean is used by SMS services for sending OTP verification messages.
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
