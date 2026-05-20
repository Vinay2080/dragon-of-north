package org.miniProjectTwo.DragonOfNorth.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionAccessTokenIssuerTest {

    private JwtServices jwtServices;
    private SessionAccessTokenIssuer issuer;

    @BeforeEach
    void setUp() {
        jwtServices = mock(JwtServices.class);
        issuer = new SessionAccessTokenIssuer(jwtServices);
    }

    @Test
    void toAuthnFacts_shouldProjectVerifiedSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant verifiedAt = Instant.parse("2026-05-20T12:00:00Z");

        AppUser user = new AppUser();
        user.setId(userId);

        Session session = new Session();
        session.setId(sessionId);
        session.setAppUser(user);
        session.setMfaRequired(false);
        session.setMfaVerifiedAt(verifiedAt);
        session.setPrimaryAmr("passwordless");

        Role role = new Role();
        role.setRoleName(RoleName.USER);

        AuthnFacts facts = SessionAccessTokenIssuer.toAuthnFacts(session, Set.of(role));

        assertEquals(userId, facts.userId());
        assertEquals(sessionId, facts.sessionId());
        assertTrue(facts.mfaVerified());
        assertEquals(verifiedAt, facts.mfaVerifiedAt());
        assertEquals(List.of("passwordless"), facts.amr());
        assertEquals(List.of(RoleName.USER.name()), facts.roles());
    }

    @Test
    void toAuthnFacts_shouldProjectUnverifiedMfaRequiredSession() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);

        Session session = new Session();
        session.setId(UUID.randomUUID());
        session.setAppUser(user);
        session.setMfaRequired(true);
        session.setMfaVerifiedAt(null);
        session.setPrimaryAmr("pwd");

        AuthnFacts facts = SessionAccessTokenIssuer.toAuthnFacts(session, Set.of());

        assertFalse(facts.mfaVerified());
        assertNull(facts.mfaVerifiedAt());
        assertEquals(List.of("pwd"), facts.amr());
    }

    @Test
    void mintAccessToken_shouldDelegateToJwtServices() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());

        Session session = new Session();
        session.setId(UUID.randomUUID());
        session.setAppUser(user);
        session.setMfaRequired(false);
        session.setMfaVerifiedAt(Instant.now());
        session.setPrimaryAmr("pwd");

        when(jwtServices.generateAccessToken(any(AuthnFacts.class))).thenReturn("jwt");

        String token = issuer.mintAccessToken(session, Set.of());

        assertEquals("jwt", token);
        ArgumentCaptor<AuthnFacts> captor = ArgumentCaptor.forClass(AuthnFacts.class);
        verify(jwtServices).generateAccessToken(captor.capture());
        assertEquals(session.getId(), captor.getValue().sessionId());
        assertTrue(captor.getValue().mfaVerified());
    }
}
