package org.miniProjectTwo.DragonOfNorth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.DeviceIdRequest;
import org.miniProjectTwo.DragonOfNorth.dto.session.response.SessionSummaryResponse;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;


    @GetMapping("/get/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SessionSummaryResponse>>> getMySessions(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<SessionSummaryResponse> sessions = sessionService.getSessionsForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping("/delete/{sessionId}")
    public ResponseEntity<ApiResponse<?>> revokeSession(
            Authentication authentication,
            @PathVariable UUID sessionId
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        sessionService.revokeSessionById(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.successMessage("session revoked"));
    }

    @PostMapping("/revoke-others")
    public ResponseEntity<ApiResponse<?>> revokeOtherSessions(
            Authentication authentication,
            @Valid @RequestBody DeviceIdRequest deviceIdRequest
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        int revokedCount = sessionService.revokeAllOtherSessions(userId, deviceIdRequest.deviceId());
        return ResponseEntity.ok(ApiResponse.successMessage("revoked " + revokedCount + " other session(s)"));
    }
}
