package org.miniProjectTwo.DragonOfNorth.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ErrorResponse;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom AuthenticationEntryPoint that returns JSON error responses for unauthenticated access attempts in a RESTful manner.
 * <p>
 * This entry point checks for CSRF-related authentication failures and returns specific error codes for those cases. For other authentication failures, it determines if the user is unauthenticated and delegates to the authentication entry point if necessary. Authenticated users that are denied access receive standard access denied error response. This approach ensures consistent API responses for security-related errors in a REST API context.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(@NonNull HttpServletRequest request,
                         HttpServletResponse response,
                         @NonNull AuthenticationException authException) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        log.debug("Authentication failed for path={}: {}", request.getRequestURI(), authException.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.INVALID_TOKEN.getCode())
                .defaultMessage(ErrorCode.INVALID_TOKEN.getDefaultMessage())
                .build();

        response.setStatus(ErrorCode.INVALID_TOKEN.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failed(errorResponse)));
    }
}

