package com.lvmp.customerstatements_springboot.exception;

import com.lvmp.customerstatements_springboot.model.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private static final String AN_ERROR_OCCURRED = "An unexpected error occurred";
    private static final String VALIDATION_FAILED = "Validation failed";
    private static final String DOCUMENT_SAVE_ERROR = "An error occurred while uploading your statement";
    private static final String DOCUMENT_NOT_FOUND = "Document not found";
    private static final String USER_NOT_FOUND = "User not found";
    private static final String USER_SERVICE_UNAVAILABLE = "User service unavailable";
    private static final String FORBIDDEN = "You do not have permission to perform this action";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);

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

    @ExceptionHandler({S3UploadException.class, DocumentSaveException.class})
    public ResponseEntity<ErrorResponse> handleUploadException(Exception e) {
        log.error(e.getMessage(), e);

        return ResponseEntity.internalServerError().body(
                ErrorResponse.builder()
                        .message(DOCUMENT_SAVE_ERROR)
                        .description(e.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDocumentNotFoundException(DocumentNotFoundException e) {
        log.warn(e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder()
                        .message(DOCUMENT_NOT_FOUND)
                        .description(e.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(UserDoesNotExist.class)
    public ResponseEntity<ErrorResponse> handleUserDoesNotExist(UserDoesNotExist e) {
        log.error(e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder()
                        .message(USER_NOT_FOUND)
                        .description(e.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException e) {
        log.warn(e.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.builder()
                        .message(FORBIDDEN)
                        .description(e.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn(e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(
                ErrorResponse.builder()
                        .message(VALIDATION_FAILED)
                        .description(e.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error(e.getMessage(), e);

        String description = "Invalid type on %s parameter".formatted(e.getName());

        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .message(VALIDATION_FAILED)
                        .description(description)
                        .build()
        );
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccessException(ResourceAccessException e) {
        log.error("Unable to reach user-service", e);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                ErrorResponse.builder()
                        .message(USER_SERVICE_UNAVAILABLE)
                        .build()
        );
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleRestClientResponseException(RestClientResponseException e) {
        log.error("user-service returned {}: {}", e.getStatusCode(), e.getResponseBodyAsString());

        if (e.getStatusCode().is5xxServerError()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                    ErrorResponse.builder()
                            .message(USER_SERVICE_UNAVAILABLE)
                            .build()
            );
        }

        return ResponseEntity.internalServerError().body(
                ErrorResponse.builder()
                        .message(AN_ERROR_OCCURRED)
                        .build()
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error(e.getMessage(), e);

        return ResponseEntity.internalServerError().body(
                ErrorResponse.builder()
                        .message(AN_ERROR_OCCURRED)
                        .build()
        );
    }
}
