package org.miniProjectTwo.DragonOfNorth.config.OtpConfig;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SesConfigTest {

    @Test
    void sesClient_shouldUseConfiguredRegion() {
        SesConfig sesConfig = new SesConfig();
        ReflectionTestUtils.setField(sesConfig, "region", "ap-south-1");

        SesClient sesClient = sesConfig.sesClient();
        assertNotNull(sesClient);
        assertEquals(Region.AP_SOUTH_1, sesClient.serviceClientConfiguration().region());

        sesClient.close();
    }
}
