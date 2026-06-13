package org.miniProjectTwo.DragonOfNorth.modules.session.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.DeviceIdRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaPolicy;
import org.miniProjectTwo.DragonOfNorth.modules.session.api.SessionApi;
import org.miniProjectTwo.DragonOfNorth.modules.session.dto.response.SessionSummaryResponse;
import org.miniProjectTwo.DragonOfNorth.modules.session.service.SessionService;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.security.web.SensitiveAccountOperation;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Session management controller for listing and revoking authenticated user sessions.
 * <p>
 * Endpoints bind actions to the caller's resolved user ID to prevent cross-account session
 * operations and to support device-aware security controls.
 */
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController implements SessionApi {

    private final SessionService sessionService;

    /**
     * Retrieves a list of active sessions for the current authenticated user.
     */
    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionSummaryResponse>>> getMySessions(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        List<SessionSummaryResponse> sessions = sessionService.getSessionsForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * Revokes a specific session by ID, ensuring the session belongs to the authenticated user.
     */
    @Override
    @DeleteMapping("/{sessionId}")
    @SensitiveAccountOperation(policy = RecentMfaPolicy.SESSION_REVOKE, onlyWhenMfaEnabled = true)
    public ResponseEntity<ApiResponse<?>> revokeSession(
            Authentication authentication,
            @PathVariable UUID sessionId
    ) {
        UUID userId = resolveUserId(authentication);
        sessionService.revokeSessionById(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.successMessage("session revoked"));
    }

    /**
     * Revokes every other session except the current device-bound session.
     */
    @Override
    @PostMapping("/revoke-others")
    @SensitiveAccountOperation(policy = RecentMfaPolicy.SESSION_REVOKE_ALL, onlyWhenMfaEnabled = true)
    public ResponseEntity<ApiResponse<?>> revokeOtherSessions(
            Authentication authentication,
            @RequestBody @Valid DeviceIdRequest deviceIdRequest
    ) {
        UUID userId = resolveUserId(authentication);
        int revokedCount = sessionService.revokeAllOtherSessions(userId, deviceIdRequest.deviceId());
        return ResponseEntity.ok(ApiResponse.successMessage("revoked " + revokedCount + " other session(s)"));
    }

    /**
     * Resolves the user ID from the authentication principal, supporting both SecurityPrincipal and direct UUID representations. Throws an exception for unsupported principal types.
     */
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
