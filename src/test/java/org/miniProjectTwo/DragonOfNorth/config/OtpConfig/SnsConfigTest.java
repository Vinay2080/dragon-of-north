package org.miniProjectTwo.DragonOfNorth.config.OtpConfig;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SnsConfigTest {

    @Test
    void snsClient_shouldUseConfiguredRegion() {
        SnsConfig snsConfig = new SnsConfig();
        ReflectionTestUtils.setField(snsConfig, "region", "ap-south-1");

        SnsClient snsClient = snsConfig.snsClient();
        assertNotNull(snsClient);
        assertEquals(Region.AP_SOUTH_1, snsClient.serviceClientConfiguration().region());

        snsClient.close();
    }
}
