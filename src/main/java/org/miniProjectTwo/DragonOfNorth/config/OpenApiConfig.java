package org.miniProjectTwo.DragonOfNorth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Central OpenAPI configuration for Dragon of North.
 *
 * <p>Defines API metadata, server entries, and security schemes that are reused
 * by all endpoints in Swagger UI.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dragon of North Authentication API")
                        .version("v1")
                        .description("REST API for identifier-based authentication, OTP verification, session management, " +
                                "JWT cookie refresh, and Redis-backed rate limiting.")
                        .contact(new Contact()
                                .name("Dragon of North")
                                .url("https://github.com/Vinay2080/dragon-of-north"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("/").description("Current environment"),
                        new Server().url("http://localhost:8080").description("Local development")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT bearer token support for tools that do not use cookies."))
                        .addSecuritySchemes("accessTokenCookie", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("access_token")
                                .description("HTTP-only cookie set after successful login."))
                        .addSecuritySchemes("refreshTokenCookie", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("refresh_token")
                                .description("HTTP-only cookie used by /auth/jwt/refresh.")))
                .addSecurityItem(new SecurityRequirement().addList("accessTokenCookie"));
    }

}
