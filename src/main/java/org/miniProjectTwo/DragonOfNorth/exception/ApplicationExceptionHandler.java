package org.miniProjectTwo.DragonOfNorth.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.api.ErrorResponse;
import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.miniProjectTwo.DragonOfNorth.dto.api.ErrorResponse.ValidationError;

/**
 * Global exception handler for consistent API error responses.
 * <p>
 * Maps exceptions to standardized ErrorResponse with proper HTTP status codes.
 * Handles validation, authentication, and business logic exceptions. Critical for
 * API contract maintenance and client error handling consistency.
 *
 * @see BusinessException for business errors
 * @see ErrorCode for error mapping
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ApplicationExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(BusinessException businessException) {
        // Builds error response from a business exception
        final ErrorResponse errorResponse = ErrorResponse.builder()
                .code(businessException.getErrorCode().getCode())
                .defaultMessage(businessException.getErrorCode().getDefaultMessage())
                .build();

        return ResponseEntity
                .status(businessException.getErrorCode().getHttpStatus())
                .body(ApiResponse.failed(errorResponse));

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(MethodArgumentNotValidException exception) {
        final List<ValidationError> list = new ArrayList<>();
        // Extracts validation errors into a structured list
        exception
                .getBindingResult()
                .getAllErrors()
                .forEach(
                        errors -> {
                            final String fieldName = ((FieldError) errors).getField();
                            final String errorCode = errors.getDefaultMessage();
                            list.add(ValidationError
                                    .builder()
                                    .field(fieldName)
                                    .code(errorCode)
                                    .message(errorCode)
                                    .build()
                            );
                        }
                );
        final ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.INVALID_INPUT.getCode())
                .validationErrorList(list)
                .build();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failed(errorResponse));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(BadCredentialsException exception) {
        log.error("Authentication failed: {}", exception.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.INVALID_TOKEN.getCode())
                .defaultMessage("Invalid username or password")
                .build();
        return ResponseEntity
                .status(ErrorCode.INVALID_TOKEN.getHttpStatus())
                .body(ApiResponse.failed(errorResponse));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleConstraintViolation(ConstraintViolationException ex) {
        log.error("Constraint violation: {}", ex.getMessage());

        List<ValidationError> errors = ex.getConstraintViolations().stream()
                .map(violation -> ValidationError.builder()
                        .field(violation.getPropertyPath().toString())
                        .code("CONSTRAINT_VIOLATION")
                        .message(violation.getMessage())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .defaultMessage("Validation failed")
                .validationErrorList(errors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failed(errorResponse));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.error("Invalid request body: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("INVALID_REQUEST_BODY")
                .defaultMessage("Invalid request body")
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failed(errorResponse));
    }


    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNoHandlerFound(NoHandlerFoundException ex) {
        log.error("No handler found: {} {}", ex.getHttpMethod(), ex.getRequestURL());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("RESOURCE_NOT_FOUND")
                .defaultMessage("The requested resource was not found")
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failed(errorResponse));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.error("Method not allowed: {}", ex.getMethod());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("METHOD_NOT_ALLOWED")
                .defaultMessage(String.format("Method %s is not supported for this endpoint", ex.getMethod()))
                .build();

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.failed(errorResponse));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAllUncaughtException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("INTERNAL_SERVER_ERROR")
                .defaultMessage("An unexpected error occurred")
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failed(errorResponse));
    }

}
