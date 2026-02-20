package org.miniProjectTwo.DragonOfNorth.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.DeviceIdRequest;
import org.miniProjectTwo.DragonOfNorth.dto.session.response.SessionSummaryResponse;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.SessionService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @InjectMocks
    private SessionController sessionController;

    @Mock
    private SessionService sessionService;

    @Mock
    private Authentication authentication;

    @Test
    void getMySessions_shouldReturnSessionsForAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        when(authentication.getPrincipal()).thenReturn(userId);

        List<SessionSummaryResponse> sessions = List.of(
                new SessionSummaryResponse(UUID.randomUUID(), "d1", "127.0.0.1", "UA", Instant.now(), Instant.now().plusSeconds(60), false)
        );
        when(sessionService.getSessionsForUser(userId)).thenReturn(sessions);

        ResponseEntity<ApiResponse<List<SessionSummaryResponse>>> response = sessionController.getMySessions(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void revokeSession_shouldInvokeServiceAndReturnSuccess() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(authentication.getPrincipal()).thenReturn(userId);

        ResponseEntity<ApiResponse<?>> response = sessionController.revokeSession(authentication, sessionId);

        verify(sessionService).revokeSessionById(userId, sessionId);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("session revoked", response.getBody().getMessage());
    }

    @Test
    void revokeOtherSessions_shouldInvokeServiceAndReturnCount() {
        UUID userId = UUID.randomUUID();
        when(authentication.getPrincipal()).thenReturn(userId);
        when(sessionService.revokeAllOtherSessions(userId, "device-1")).thenReturn(2);

        ResponseEntity<ApiResponse<?>> response = sessionController.revokeOtherSessions(authentication, new DeviceIdRequest("device-1"));

        verify(sessionService).revokeAllOtherSessions(userId, "device-1");
        assertEquals(200, response.getStatusCode().value());
        assertEquals("revoked 2 other session(s)", response.getBody().getMessage());
    }
}
