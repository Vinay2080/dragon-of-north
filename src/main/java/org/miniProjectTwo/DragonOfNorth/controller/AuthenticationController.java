package org.miniProjectTwo.DragonOfNorth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserLoginRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpCompleteRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserStatusFinderRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.services.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.services.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

/**
 * REST controller for authentication operations.
 * Provides endpoints for user authentication including status checking, registration,
 * login, and token refresh. Supports multiple authentication methods (email, phone)
 * through the AuthenticationServiceResolver and handles JWT token management.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationServiceResolver resolver;
    private final AuthCommonServices authCommonServices;

    /**
     * Checks the status of a user identifier.
     * Determines if the user exists and their current registration status.
     * Useful for frontend to determine what authentication steps are needed.
     *
     * @param request the user identifier request containing identifier and type
     * @return response with user status information
     */
    @PostMapping("/identifier/status")
    public ResponseEntity<ApiResponse<AppUserStatusFinderResponse>> findUserStatus(
            @RequestBody
            @Valid
            AppUserStatusFinderRequest request
    ) {
        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.getUserStatus(request.identifier());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Initiates user registration process.
     * Starts the sign-up flow by creating a user account in CREATED status.
     * The user will need to complete verification before being fully registered.
     *
     * @param request the sign-up request with user details
     * @return response with registration status and next steps
     */
    @PostMapping("/identifier/sign-up")
    public ResponseEntity<ApiResponse<AppUserStatusFinderResponse>> signupUser(
            @RequestBody
            @Valid
            AppUserSignUpRequest request
    ) {

        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.signUpUser(request);
        return ResponseEntity.status(CREATED).body(ApiResponse.success(response));
    }

    /**
     * Completes the user registration process.
     * Finalizes the sign-up process by verifying the user and upgrading
     * their status to VERIFIED, enabling full authentication capabilities.
     *
     * @param request the completion request with user identifier
     * @return response with final registration status
     */
    @PostMapping("/identifier/sign-up/complete")
    public ResponseEntity<ApiResponse<AppUserStatusFinderResponse>> completeUserSignup(
            @RequestBody
            @Valid
            AppUserSignUpCompleteRequest request
    ) {
        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.completeSignUp(request.identifier());
        return ResponseEntity.status(CREATED).body(ApiResponse.success(response));
    }

    /**
     * Authenticates a user and issues JWT tokens.
     * Validates user credentials and sets secure HTTP-only cookies containing
     * access and refresh tokens. The access token is short-lived while the refresh
     * token can be used to get new access tokens.
     *
     * @param request             the login request with identifier and password
     * @param httpServletResponse the response for setting secure cookies
     * @return success message indicating authentication was successful
     */
    @PostMapping("/identifier/login")
    public ResponseEntity<ApiResponse<?>> loginUser(
            @RequestBody
            @Valid
            AppUserLoginRequest request,
            HttpServletResponse httpServletResponse
    ) {
        authCommonServices.login(request.identifier(), request.password(), httpServletResponse);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.successMessage("log in successful"));
    }

    /**
     * Refreshes JWT access tokens using a valid refresh token.
     * Extracts the refresh token from HTTP-only cookies, validates it,
     * and issues a new access token. Maintains user session without requiring
     * re-authentication.
     *
     * @param request  the HTTP request containing refresh token cookie
     * @param response the HTTP response for setting a new access token cookie
     * @return success message indicating token refresh was successful
     */
    @PostMapping("/jwt/refresh")
    public ResponseEntity<ApiResponse<?>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authCommonServices.refreshToken(request, response);
        return ResponseEntity.ok(ApiResponse.successMessage("refresh token sent"));
    }
}
