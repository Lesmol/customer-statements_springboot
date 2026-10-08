package com.lvmp.loginservice.exception;

import com.lvmp.loginservice.model.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private static final String AN_ERROR_OCCURRED = "An unexpected error occurred";
    private static final String VALIDATION_FAILED = "Validation failed";
    private static final String AUTHENTICATION_FAILED = "Authentication failed";
    private static final String AUTHENTICATION_SERVICE_UNAVAILABLE = "Authentication service unavailable";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn(e.getMessage());

        String validationDetails = e.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .message(VALIDATION_FAILED)
                        .description(validationDetails)
                        .build()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn(e.getMessage());

        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .message(VALIDATION_FAILED)
                        .description(e.getMessage())
                        .build());
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleRestClientResponseException(RestClientResponseException e) {
        if (e.getStatusCode().is5xxServerError()) {
            log.error("Dex returned a server error: {}", e.getResponseBodyAsString(), e);

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                    ErrorResponse.builder()
                            .message(AUTHENTICATION_SERVICE_UNAVAILABLE)
                            .build());
        }

        log.warn("Dex rejected the login attempt: {}", e.getResponseBodyAsString());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .message(AUTHENTICATION_FAILED)
                        .description("Incorrect username or password")
                        .build());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccessException(ResourceAccessException e) {
        log.error("Unable to reach Dex", e);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                ErrorResponse.builder()
                        .message(AUTHENTICATION_SERVICE_UNAVAILABLE)
                        .build());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error(e.getMessage(), e);

        return ResponseEntity.internalServerError().body(
                ErrorResponse.builder()
                        .message(AN_ERROR_OCCURRED)
                        .build());
    }
}