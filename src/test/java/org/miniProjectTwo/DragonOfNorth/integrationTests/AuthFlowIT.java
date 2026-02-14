package org.miniProjectTwo.DragonOfNorth.integrationTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserLoginRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpCompleteRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthFlowIT extends BaseIntegrationTest {

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

        // 4. Extract cookies containing access and refresh tokens
        jakarta.servlet.http.Cookie accessCookie = loginResult.getResponse().getCookie("access_token");
        jakarta.servlet.http.Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");
        assertThat(accessCookie).isNotNull();
        assertThat(refreshCookie).isNotNull();

        String accessTokenCookie = accessCookie.getValue();
        String refreshTokenCookie = refreshCookie.getValue();
        assertThat(accessTokenCookie).isNotBlank();
        assertThat(refreshTokenCookie).isNotBlank();

        // 5. Access protected endpoint with JWT (expect 404 but authenticated)
        mockMvc.perform(get("/api/v1/protected-test")
                        .header("Authorization", "Bearer " + accessTokenCookie))
                .andExpect(status().isNotFound());

        // 6. Access the same endpoint without JWT (expect 401 or 403)
        mockMvc.perform(get("/api/v1/protected-test"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));

        // 7. Test multi-device login: login from another "device" (creates new refresh token)
        MvcResult secondLoginResult = mockMvc.perform(post("/api/v1/auth/identifier/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        jakarta.servlet.http.Cookie secondAccessCookie = secondLoginResult.getResponse().getCookie("access_token");
        jakarta.servlet.http.Cookie secondRefreshCookie = secondLoginResult.getResponse().getCookie("refresh_token");
        assertThat(secondAccessCookie).isNotNull();
        assertThat(secondRefreshCookie).isNotNull();

        String secondAccessTokenCookie = secondAccessCookie.getValue();
        String secondRefreshTokenCookie = secondRefreshCookie.getValue();
        assertThat(secondAccessTokenCookie).isNotBlank();
        assertThat(secondRefreshTokenCookie).isNotBlank();
        assertThat(secondRefreshTokenCookie).isNotEqualTo(refreshTokenCookie);

        // 8. Refresh token from first session
        mockMvc.perform(post("/api/v1/auth/jwt/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshTokenCookie)))
                .andExpect(status().isOk());

        // 9. Logout from first session
        mockMvc.perform(post("/api/v1/auth/identifier/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshTokenCookie)))
                .andExpect(status().isOk());

        // 10. Verify first session refresh token is revoked
        mockMvc.perform(post("/api/v1/auth/jwt/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshTokenCookie)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));

        // 11. Verify second session is still active
        mockMvc.perform(post("/api/v1/auth/jwt/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", secondRefreshTokenCookie)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldHandleMultipleDeviceLoginsIndependently() throws Exception {
        // Setup: Ensure role exists
        if (!roleRepository.existsByRoleName(RoleName.USER)) {
            Role userRole = new Role();
            userRole.setRoleName(RoleName.USER);
            userRole.setSystemRole(true);
            roleRepository.save(userRole);
        }

        String email = "multidevice@example.com";
        String password = "Password123!";

        // 1. Create and verify user
        AppUserSignUpRequest signupRequest = new AppUserSignUpRequest(email, IdentifierType.EMAIL, password);
        mockMvc.perform(post("/api/v1/auth/identifier/sign-up")
                        .header("X-Forwarded-For", "127.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        AppUserSignUpCompleteRequest completeRequest = new AppUserSignUpCompleteRequest(email, IdentifierType.EMAIL);
        mockMvc.perform(post("/api/v1/auth/identifier/sign-up/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isCreated());

        // 2. Login from Device 1
        AppUserLoginRequest loginRequest = new AppUserLoginRequest(email, password);
        MvcResult device1Login = mockMvc.perform(post("/api/v1/auth/identifier/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        jakarta.servlet.http.Cookie device1Cookie = device1Login.getResponse().getCookie("refresh_token");
        assertThat(device1Cookie).isNotNull();
        String device1RefreshToken = device1Cookie.getValue();

        // 3. Login from Device 2
        MvcResult device2Login = mockMvc.perform(post("/api/v1/auth/identifier/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        jakarta.servlet.http.Cookie device2Cookie = device2Login.getResponse().getCookie("refresh_token");
        assertThat(device2Cookie).isNotNull();
        String device2RefreshToken = device2Cookie.getValue();

        // 4. Login from Device 3
        MvcResult device3Login = mockMvc.perform(post("/api/v1/auth/identifier/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        jakarta.servlet.http.Cookie device3Cookie = device3Login.getResponse().getCookie("refresh_token");
        assertThat(device3Cookie).isNotNull();
        String device3RefreshToken = device3Cookie.getValue();

        // 5. Verify all three refresh tokens are different
        assertThat(device1RefreshToken).isNotEqualTo(device2RefreshToken);
        assertThat(device2RefreshToken).isNotEqualTo(device3RefreshToken);
        assertThat(device1RefreshToken).isNotEqualTo(device3RefreshToken);

        // 6. Verify all three sessions can refresh tokens
        mockMvc.perform(post("/api/v1/auth/jwt/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", device1RefreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/jwt/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", device2RefreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/jwt/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", device3RefreshToken)))
                .andExpect(status().isOk());

        // 7. Logout from Device 2
        mockMvc.perform(post("/api/v1/auth/identifier/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", device2RefreshToken)))
                .andExpect(status().isOk());

        // 8. Verify Device 2 session is revoked
        mockMvc.perform(post("/api/v1/auth/jwt/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", device2RefreshToken)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));

        // 9. Verify Device 1 and Device 3 are still active
        mockMvc.perform(post("/api/v1/auth/jwt/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", device1RefreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/jwt/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", device3RefreshToken)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectLogoutWithInvalidToken() throws Exception {
        // Attempt logout with non-existent refresh token
        mockMvc.perform(post("/api/v1/auth/identifier/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "invalid-token-12345")))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    void shouldRejectRefreshWithRevokedToken() throws Exception {
        // Setup: Ensure role exists
        if (!roleRepository.existsByRoleName(RoleName.USER)) {
            Role userRole = new Role();
            userRole.setRoleName(RoleName.USER);
            userRole.setSystemRole(true);
            roleRepository.save(userRole);
        }

        String email = "revoked-test@example.com";
        String password = "Password123!";

        // Create and verify user
        AppUserSignUpRequest signupRequest = new AppUserSignUpRequest(email, IdentifierType.EMAIL, password);
        mockMvc.perform(post("/api/v1/auth/identifier/sign-up")
                        .header("X-Forwarded-For", "127.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        AppUserSignUpCompleteRequest completeRequest = new AppUserSignUpCompleteRequest(email, IdentifierType.EMAIL);
        mockMvc.perform(post("/api/v1/auth/identifier/sign-up/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isCreated());

        // Login
        AppUserLoginRequest loginRequest = new AppUserLoginRequest(email, password);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/identifier/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        jakarta.servlet.http.Cookie refreshCookieForRevoke = loginResult.getResponse().getCookie("refresh_token");
        assertThat(refreshCookieForRevoke).isNotNull();
        String refreshToken = refreshCookieForRevoke.getValue();

        // Logout (revokes token)
        mockMvc.perform(post("/api/v1/auth/identifier/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk());

        // Attempt to use revoked token
        mockMvc.perform(post("/api/v1/auth/jwt/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshToken)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }
}
