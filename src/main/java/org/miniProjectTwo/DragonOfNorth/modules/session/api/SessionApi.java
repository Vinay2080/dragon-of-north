package org.miniProjectTwo.DragonOfNorth.modules.session.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.DeviceIdRequest;
import org.miniProjectTwo.DragonOfNorth.modules.session.dto.response.SessionSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

@Tag(name = "Sessions", description = "View and revoke device sessions for the authenticated user.")
@SecurityRequirement(name = "accessTokenCookie")
public interface SessionApi {

    @Operation(
            summary = "List my sessions",
            description = "Returns the current user's active and revoked device sessions for account security screens."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Sessions returned",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "sessionsFound",
                                    value = """
                                            {
                                              "api_response_status": "success",
                                              "data": [
                                                {
                                                  "session_id": "a987ab67-14b7-4f1e-b77f-cfd61133cc3b",
                                                  "device_id": "web-chrome-macos",
                                                  "ip_address": "203.0.113.10",
                                                  "user_agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_3)",
                                                  "last_used_at": "2026-04-04T06:30:00Z",
                                                  "expiry_date": "2026-04-11T06:30:00Z",
                                                  "revoked": false
                                                }
                                              ],
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Authentication is required")
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<List<SessionSummaryResponse>>> getMySessions(
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(
            summary = "Revoke one session",
            description = "Revokes a specific session owned by the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Session revoked",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "sessionRevoked",
                                    value = """
                                            {
                                              "message": "session revoked",
                                              "api_response_status": "success",
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Session was not found for the current user")
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> revokeSession(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(
                    description = "Session identifier to revoke.",
                    example = "a987ab67-14b7-4f1e-b77f-cfd61133cc3b"
            )
            UUID sessionId
    );

    @Operation(
            summary = "Revoke other sessions",
            description = "Revokes every session owned by the current user except the device identified in the request body."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Other sessions revoked",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "otherSessionsRevoked",
                                    value = """
                                            {
                                              "message": "revoked 2 other session(s)",
                                              "api_response_status": "success",
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "400", description = "Device identifier is missing or invalid")
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> revokeOtherSessions(
            @Parameter(hidden = true) Authentication authentication,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Device identifier for the session that should remain active.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "revokeOtherSessionsRequest",
                                    value = """
                                            {
                                              "device_id": "web-chrome-macos"
                                            }
                                            """
                            )
                    )
            )
            DeviceIdRequest deviceIdRequest
    );
}
