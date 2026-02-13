package org.miniProjectTwo.DragonOfNorth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for OpenAPI (Swagger) documentation.
 * <p>
 * Configures the OpenAPI specification for the Dragon of North application,
 * providing API documentation with metadata, licensing information, and server details.
 * This configuration enables automatic generation of interactive API documentation
 * for the authentication and user management endpoints.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates and configures the custom OpenAPI bean for API documentation.
     * Sets up the API metadata including title, version, description, and licensing.
     * Configures the default server URL for the API endpoints.
     *
     * @return a configured OpenAPI instance with Dragon of North API specifications
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dragon of North API")
                        .version("1.0")
                        .description("API documentation for Dragon of North application")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")))
                .servers(List.of(
                        new Server().url("/").description("Default server URL")
                ));
    }

}
