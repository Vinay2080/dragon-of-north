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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Converts authenticated-but-forbidden requests into standardized API error responses.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    public void handle(@NonNull HttpServletRequest request,
                       @NonNull HttpServletResponse response,
                       @NonNull AccessDeniedException accessDeniedException) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        if (isCsrfFailure(accessDeniedException)) {
            writeForbidden(response, ErrorCode.CSRF_INVALID);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean unauthenticated = authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken;

        if (unauthenticated) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new InsufficientAuthenticationException("Full authentication is required")
            );
            return;
        }

        log.debug("Access denied for authenticated user on path={}: {}",
                request.getRequestURI(), accessDeniedException.getMessage());
        writeForbidden(response, ErrorCode.ACCESS_DENIED);
    }

    private static boolean isCsrfFailure(AccessDeniedException accessDeniedException) {
        return accessDeniedException instanceof MissingCsrfTokenException
                || accessDeniedException instanceof InvalidCsrfTokenException;
    }

    private void writeForbidden(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(errorCode.getCode())
                .defaultMessage(errorCode.getDefaultMessage())
                .build();

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failed(errorResponse)));
    }
}


