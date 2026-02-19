package org.miniProjectTwo.DragonOfNorth.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.DeviceIdRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.SessionSummaryResponse;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.SessionService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @InjectMocks
    private SessionController sessionController;

    @Mock
    private SessionService sessionService;

    @Test
    void getMySessions_shouldReturnSuccess() {
        UUID userId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());

        SessionSummaryResponse item = new SessionSummaryResponse(
                UUID.randomUUID(), "device-1", "127.0.0.1", "UA", Instant.now(), Instant.now().plusSeconds(60), false, true
        );
        when(sessionService.getSessionsForUser(userId, "device-1")).thenReturn(List.of(item));

        ResponseEntity<ApiResponse<List<SessionSummaryResponse>>> response = sessionController.getMySessions(auth, "device-1");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        verify(sessionService).getSessionsForUser(userId, "device-1");
    }

    @Test
    void revokeSession_shouldCallService() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());

        ResponseEntity<ApiResponse<?>> response = sessionController.revokeSession(auth, sessionId);

        assertEquals(200, response.getStatusCode().value());
        verify(sessionService).revokeSessionById(userId, sessionId);
    }

    @Test
    void revokeOtherSessions_shouldReturnCountMessage() {
        UUID userId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        when(sessionService.revokeAllOtherSessions(userId, "device-1")).thenReturn(2);

        ResponseEntity<ApiResponse<?>> response = sessionController.revokeOtherSessions(auth, new DeviceIdRequest("device-1"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("revoked 2 other session(s)", response.getBody().getMessage());
        verify(sessionService).revokeAllOtherSessions(userId, "device-1");
    }
}
