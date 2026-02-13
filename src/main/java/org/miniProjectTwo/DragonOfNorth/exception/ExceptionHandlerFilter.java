package org.miniProjectTwo.DragonOfNorth.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.api.ErrorResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter exception handler for BusinessException in filter chains.
 * <p>
 * Catches business exceptions before controller advice and returns standardized
 * JSON responses. Runs at Order(0) to intercept exceptions early. Critical for
 * consistent error handling in rate limiting and authentication filters.
 *
 * @see BusinessException for caught exceptions
 * @see ApplicationExceptionHandler for controller-level handling
 */
@Component
@Order(0) // Must run BEFORE RateLimitFilter
@RequiredArgsConstructor
@Slf4j
public class ExceptionHandlerFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            log.error("BusinessException in filter chain: code={}, message={}",
                    e.getErrorCode().getCode(), e.getMessage());

            // Build ErrorResponse matching your existing format
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .code(e.getErrorCode().getCode())
                    .defaultMessage(e.getErrorCode().getDefaultMessage())
                    .build();

            // Wrap in ApiResponse.failed() like your controller advice does
            ApiResponse<ErrorResponse> apiResponse = ApiResponse.failed(errorResponse);

            // Set HTTP status and content type
            response.setStatus(e.getErrorCode().getHttpStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            // Write JSON response
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        }
    }
}