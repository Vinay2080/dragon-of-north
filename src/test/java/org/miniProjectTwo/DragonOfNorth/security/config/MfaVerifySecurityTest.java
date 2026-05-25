package org.miniProjectTwo.DragonOfNorth.security.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MfaVerifySecurityTest {

    @Test
    void csrfBypassUrls_shouldContainMfaVerifyEndpoint() {
        boolean present = Arrays.asList(SecurityConfig.csrf_bypass_urls)
                .contains("/api/v1/auth/mfa/verify");

        assertTrue(present, "CSRF bypass should include /api/v1/auth/mfa/verify");
    }
}


