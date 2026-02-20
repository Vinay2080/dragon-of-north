package org.miniProjectTwo.DragonOfNorth;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.integrationTests.BaseIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@Disabled
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class DragonOfNorthApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {
    }

}
