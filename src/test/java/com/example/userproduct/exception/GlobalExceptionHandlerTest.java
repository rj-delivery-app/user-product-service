package com.example.userproduct.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test @DisplayName("handleValidationErrors returns BAD_REQUEST")
    void handleValidationErrors_returnsBadRequest() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "Email is required"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getValidationErrors()).hasSize(1);
    }

    @Test @DisplayName("handleIllegalArgument returns BAD_REQUEST")
    void handleIllegalArgument_returnsBadRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(new IllegalArgumentException("Invalid input"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid input");
    }

    @Test @DisplayName("handleIllegalState returns CONFLICT")
    void handleIllegalState_returnsConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalState(new IllegalStateException("Invalid state"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test @DisplayName("handleAuthenticationErrors returns UNAUTHORIZED")
    void handleAuthenticationErrors_returnsUnauthorized() {
        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationErrors(new BadCredentialsException("Bad credentials"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test @DisplayName("handleAuthenticationErrors with UsernameNotFoundException returns UNAUTHORIZED")
    void handleAuthenticationErrors_usernameNotFound_returnsUnauthorized() {
        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationErrors(new UsernameNotFoundException("User not found"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test @DisplayName("handleGenericException returns INTERNAL_SERVER_ERROR")
    void handleGenericException_returnsInternalServerError() {
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(new RuntimeException("Something unexpected"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }
}
