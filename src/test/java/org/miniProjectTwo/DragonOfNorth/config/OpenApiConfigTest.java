package org.miniProjectTwo.DragonOfNorth.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void customOpenAPI_shouldReturnConfiguredMetadata() {
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        assertNotNull(openAPI);
        assertEquals("Dragon of North API", openAPI.getInfo().getTitle());
        assertEquals("1.0", openAPI.getInfo().getVersion());
        assertFalse(openAPI.getServers().isEmpty());
        assertEquals("/", openAPI.getServers().get(0).getUrl());
    }
}
