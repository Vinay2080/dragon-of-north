package org.miniProjectTwo.DragonOfNorth.modules.auth.controller;

import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CsrfControllerTest {

    private final CsrfController csrfController = new CsrfController();

    @Test
    void csrf_shouldReturnTokenFromCsrfObject() {
        CsrfToken csrfToken = new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "csrf-token-value");

        ResponseEntity<ApiResponse<Map<String, String>>> response = csrfController.csrf(csrfToken);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).containsEntry("token", "csrf-token-value");
    }
}
