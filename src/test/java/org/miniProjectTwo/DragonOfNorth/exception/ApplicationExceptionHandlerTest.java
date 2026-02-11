package org.miniProjectTwo.DragonOfNorth.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.api.ErrorResponse;
import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationExceptionHandlerTest {

    @InjectMocks
    private ApplicationExceptionHandler exceptionHandler;

    @Test
    void handleBusinessException_ShouldReturnCorrectResponse() {
        // Given
        BusinessException ex = new BusinessException(ErrorCode.USER_NOT_FOUND);

        // When
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleException(ex);
        assert response.getBody() != null;
        ErrorResponse errorResponse = response.getBody().getData();

        // Then
        assertEquals(ErrorCode.USER_NOT_FOUND.getHttpStatus(), response.getStatusCode());
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), errorResponse.code());
        assertEquals(ErrorCode.USER_NOT_FOUND.getDefaultMessage(), errorResponse.defaultMessage());
    }

    @Test
    void handleMethodArgumentNotValid_ShouldReturnValidationErrors() throws NoSuchMethodException {
        // Given
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(null, "object");
        bindingResult.addError(new FieldError("object", "username", "must not be blank"));
        bindingResult.addError(new FieldError("object", "email", "must be a valid email"));

        // Create a mock MethodParameter
        Method method = this.getClass().getDeclaredMethod("handleMethodArgumentNotValid_ShouldReturnValidationErrors");
        MethodParameter parameter = new MethodParameter(method, -1); // -1 for return type

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        // When
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleException(ex);
        assert response.getBody() != null;
        ErrorResponse errorResponse = response.getBody().getData();
        List<ErrorResponse.ValidationError> errors = errorResponse.validationErrorList();

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.INVALID_INPUT.getCode(), errorResponse.code());
        assertEquals(2, errors.size());
        // Add more assertions as needed
    }
    @Test
    void handleBadCredentialsException_ShouldReturnUnauthorized() {
        // Given
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        // When
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleException(ex);
        assertNotNull(response.getBody(), "Response body should not be null");
        ErrorResponse errorResponse = response.getBody().getData();

        // Then
        assertEquals(ErrorCode.INVALID_TOKEN.getHttpStatus(), response.getStatusCode());
        assertEquals(ErrorCode.INVALID_TOKEN.getCode(), errorResponse.code());
        assertEquals("Invalid username or password", errorResponse.defaultMessage());
    }

    @Test
    void handleConstraintViolation_ShouldReturnValidationErrors() {
        // Given
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("email");
        when(violation.getMessage()).thenReturn("must be a valid email");

        ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

        // When
        ResponseEntity<ApiResponse<ErrorResponse>> response =
                exceptionHandler.handleConstraintViolation(ex);
        assertNotNull(response.getBody(), "Response body should not be null");
        ErrorResponse errorResponse = response.getBody().getData();
        List<ErrorResponse.ValidationError> errors = errorResponse.validationErrorList();

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", errorResponse.code());
        assertEquals(1, errors.size());
        assertEquals("email", errors.getFirst().getField());
        assertEquals("must be a valid email", errors.getFirst().getMessage());
    }

    @Test
    void handleHttpMessageNotReadable_ShouldReturnBadRequest() {
        // Given
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Invalid JSON", null, new MockHttpInputMessage(new byte[0]));

        // When
        ResponseEntity<ApiResponse<ErrorResponse>> response =
                exceptionHandler.handleHttpMessageNotReadable(ex);

        assertNotNull(response.getBody(), "Response body should not be null");

        ErrorResponse errorResponse = response.getBody().getData();

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_REQUEST_BODY", errorResponse.code());
        assertEquals("Invalid request body", errorResponse.defaultMessage());
    }


    @Test
    void handleNoHandlerFound_ShouldReturnNotFound() {
        // Given
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/api/nonexistent", new HttpHeaders());

        // When
        ResponseEntity<ApiResponse<ErrorResponse>> response =
                exceptionHandler.handleNoHandlerFound(ex);
        assertNotNull(response.getBody(), "Response body should not be null");
        ErrorResponse errorResponse = response.getBody().getData();

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("RESOURCE_NOT_FOUND", errorResponse.code());
        assertEquals("The requested resource was not found", errorResponse.defaultMessage());
    }

    @Test
    void handleHttpRequestMethodNotSupported_ShouldReturnMethodNotAllowed() {
        // Given
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("PATCH");

        // When
        ResponseEntity<ApiResponse<ErrorResponse>> response =
                exceptionHandler.handleMethodNotSupported(ex);
        assertNotNull(response.getBody(), "Response body should not be null");
        ErrorResponse errorResponse = response.getBody().getData();

        // Then
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals("METHOD_NOT_ALLOWED", errorResponse.code());
        assertTrue(errorResponse.defaultMessage().contains("PATCH"));
    }


    @Test
    void handleAllUncaughtException_ShouldReturnInternalServerError() {
        // Given
        Exception ex = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<ApiResponse<ErrorResponse>> response =
                exceptionHandler.handleAllUncaughtException(ex);
        assertNotNull(response.getBody(), "Response body should not be null");
        ErrorResponse errorResponse = response.getBody().getData();

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", errorResponse.code());
        assertEquals("An unexpected error occurred", errorResponse.defaultMessage());
    }
}