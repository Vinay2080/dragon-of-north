package org.miniProjectTwo.DragonOfNorth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.DeviceIdRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.SessionSummaryResponse;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "Session Management", description = "Manage active sessions for the authenticated account")
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    @Operation(
            summary = "List sessions for current user",
            description = "Returns all sessions associated with authenticated user sorted by last used timestamp. " +
                    "Provide current device id in query to mark current session in response.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sessions fetched successfully"),
                    @ApiResponse(responseCode = "401", description = "Authentication required")
            }
    )
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse<List<SessionSummaryResponse>>> getMySessions(
            Authentication authentication,
            @RequestParam(name = "device_id", required = false) String currentDeviceId
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<SessionSummaryResponse> sessions = sessionService.getSessionsForUser(userId, currentDeviceId);
        return ResponseEntity.ok(org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse.success(sessions));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(
            summary = "Revoke a specific session",
            description = "Revokes target session if it belongs to the currently authenticated user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Session revoked"),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "400", description = "Session not found")
            }
    )
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse<?>> revokeSession(
            Authentication authentication,
            @PathVariable UUID sessionId
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        sessionService.revokeSessionById(userId, sessionId);
        return ResponseEntity.ok(org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse.successMessage("session revoked"));
    }

    @PostMapping("/revoke-others")
    @Operation(
            summary = "Revoke all sessions except current device",
            description = "Provide the current device id in body. Active session on this device remains; all others are revoked.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = DeviceIdRequest.class),
                            examples = @ExampleObject(value = "{\"device_id\":\"browser-device-123\"}")
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Other sessions revoked"),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            }
    )
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse<?>> revokeOtherSessions(
            Authentication authentication,
            @Valid @RequestBody DeviceIdRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        int revokedCount = sessionService.revokeAllOtherSessions(userId, request.deviceId());
        return ResponseEntity.ok(org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse.successMessage(
                "revoked " + revokedCount + " other session(s)"
        ));
    }
}
