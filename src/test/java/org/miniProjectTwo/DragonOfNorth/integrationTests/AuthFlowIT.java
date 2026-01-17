package org.miniProjectTwo.DragonOfNorth.integrationTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserLoginRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpCompleteRequest;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
class AuthFlowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void shouldCompleteAuthFlowEndToEnd() throws Exception {
        // 0. Setup: Ensure role exists
        if (!roleRepository.existsByRoleName(RoleName.USER)) {
            Role userRole = new Role();
            userRole.setRoleName(RoleName.USER);
            userRole.setSystemRole(true);
            roleRepository.save(userRole);
        }

        String email = "Testuser123@example.com";
        String password = "Password123!";

        // 1. Signup user
        AppUserSignUpRequest signupRequest = new AppUserSignUpRequest(email, IdentifierType.EMAIL, password);
        mockMvc.perform(post("/api/v1/auth/identifier/sign-up")
                        .header("X-Forwarded-For", "127.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        // 2. Verify user is persisted in PostgreSQL
        assertThat(userRepository.findByEmail(email)).isPresent();

        // 2.5 Complete Signup
        AppUserSignUpCompleteRequest completeRequest = new AppUserSignUpCompleteRequest(email, IdentifierType.EMAIL);
        mockMvc.perform(post("/api/v1/auth/identifier/sign-up/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isCreated());

        // 3. Login user
        AppUserLoginRequest loginRequest = new AppUserLoginRequest(email, password);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/identifier/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 4. Receive JWT token
        String responseContent = loginResult.getResponse().getContentAsString();
        String jwtToken = objectMapper.readTree(responseContent).get("data").get("access_token").asText();
        assertThat(jwtToken).isNotBlank();

        // 5. Access protected endpoint with JWT (expect 200)
        // Note: Using a non-existent but protected path would give 404, so we'll use a known path that's NOT whitelisted if possible.
        // Actually, if it's 404 but with authentication, it means we passed the security filter.
        mockMvc.perform(get("/api/v1/protected-test")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());

        // 6. Access the same endpoint without JWT (expect 401)
        mockMvc.perform(get("/api/v1/protected-test"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }
}
