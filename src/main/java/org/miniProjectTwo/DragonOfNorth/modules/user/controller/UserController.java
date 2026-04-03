package org.miniProjectTwo.DragonOfNorth.modules.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Manage account lifecycle")
@SecurityRequirement(name = "accessTokenCookie")
public class UserController {

    private final AuthCommonServices authCommonServices;

    @DeleteMapping("/me")
    @Operation(summary = "Delete current user", description = "Soft-deletes the authenticated account and revokes active sessions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Operation not allowed for account state")
    })
    public ResponseEntity<Void> deleteCurrentUser(HttpServletRequest request,
                                                  HttpServletResponse response,
                                                  @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(request, deviceId);
        authCommonServices.deleteCurrentUser(context, response);
        return ResponseEntity.noContent().build();
    }
}
