package org.miniProjectTwo.DragonOfNorth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.IdentifierEmail;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.impl.AuthenticationServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationServiceImpl authenticationService;

    @PostMapping("/identifier/email")

    public ResponseEntity<ApiResponse<?>> statusIdentifier(
            @RequestBody
            @Valid
            IdentifierEmail identifierEmail
    ) {
        AppUserStatus appUserStatus = authenticationService.emailStatusIdentifier(identifierEmail);
        return ResponseEntity.ok(ApiResponse.success(appUserStatus));
    }
}
