package org.miniProjectTwo.DragonOfNorth;

import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.integrationTests.BaseIntegrationTest;
import org.miniProjectTwo.DragonOfNorth.integrationTests.EnabledIfDockerAvailable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfDockerAvailable
class DragonOfNorthApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {
    }

}
