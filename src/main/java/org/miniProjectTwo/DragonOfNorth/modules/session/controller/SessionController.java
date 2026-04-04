package org.miniProjectTwo.DragonOfNorth.modules.session.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.DeviceIdRequest;
import org.miniProjectTwo.DragonOfNorth.modules.session.api.SessionApi;
import org.miniProjectTwo.DragonOfNorth.modules.session.dto.response.SessionSummaryResponse;
import org.miniProjectTwo.DragonOfNorth.modules.session.service.SessionService;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController implements SessionApi {

    private final SessionService sessionService;

    @Override
    @GetMapping("/get/all")
    public ResponseEntity<ApiResponse<List<SessionSummaryResponse>>> getMySessions(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<SessionSummaryResponse> sessions = sessionService.getSessionsForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @Override
    @DeleteMapping("/delete/{sessionId}")
    public ResponseEntity<ApiResponse<?>> revokeSession(
            Authentication authentication,
            @PathVariable UUID sessionId
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        sessionService.revokeSessionById(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.successMessage("session revoked"));
    }

    @Override
    @PostMapping("/revoke-others")
    public ResponseEntity<ApiResponse<?>> revokeOtherSessions(
            Authentication authentication,
            @RequestBody @Valid DeviceIdRequest deviceIdRequest
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        int revokedCount = sessionService.revokeAllOtherSessions(userId, deviceIdRequest.deviceId());
        return ResponseEntity.ok(ApiResponse.successMessage("revoked " + revokedCount + " other session(s)"));
    }
}
