package org.miniProjectTwo.DragonOfNorth.modules.session.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.DeviceIdRequest;
import org.miniProjectTwo.DragonOfNorth.modules.session.api.SessionApi;
import org.miniProjectTwo.DragonOfNorth.modules.session.dto.response.SessionSummaryResponse;
import org.miniProjectTwo.DragonOfNorth.modules.session.service.SessionService;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
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
        UUID userId = resolveUserId(authentication);
        List<SessionSummaryResponse> sessions = sessionService.getSessionsForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @Override
    @DeleteMapping("/delete/{sessionId}")
    public ResponseEntity<ApiResponse<?>> revokeSession(
            Authentication authentication,
            @PathVariable UUID sessionId
    ) {
        UUID userId = resolveUserId(authentication);
        sessionService.revokeSessionById(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.successMessage("session revoked"));
    }

    @Override
    @PostMapping("/revoke-others")
    public ResponseEntity<ApiResponse<?>> revokeOtherSessions(
            Authentication authentication,
            @RequestBody @Valid DeviceIdRequest deviceIdRequest
    ) {
        UUID userId = resolveUserId(authentication);
        int revokedCount = sessionService.revokeAllOtherSessions(userId, deviceIdRequest.deviceId());
        return ResponseEntity.ok(ApiResponse.successMessage("revoked " + revokedCount + " other session(s)"));
    }

    private UUID resolveUserId(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof SecurityPrincipal securityPrincipal && securityPrincipal.userId() != null) {
            return securityPrincipal.userId();
        }
        if (principal instanceof UUID userId) {
            return userId;
        }
        throw new IllegalStateException("Unsupported authentication principal");
    }
}
