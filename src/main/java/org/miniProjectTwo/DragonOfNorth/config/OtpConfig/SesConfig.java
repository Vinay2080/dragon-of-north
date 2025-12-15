package org.miniProjectTwo.DragonOfNorth.config.OtpConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration

public class SesConfig {

    @Value("${aws.region}")
    private String region;


    @Bean
    public SesClient sesClient() {
        return SesClient
                .builder()
                .region(Region.of(region))
                .build();
    }

}