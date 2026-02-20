package org.miniProjectTwo.DragonOfNorth.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JpaConfigTest {

    @Test
    void constructor_shouldInitializeConfig() {
        assertNotNull(new JpaConfig());
    }
}
