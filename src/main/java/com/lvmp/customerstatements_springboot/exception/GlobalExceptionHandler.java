package com.lvmp.customerstatements_springboot.exception;

import com.lvmp.customerstatements_springboot.model.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.IOException;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private static final String AN_ERROR_OCCURRED = "An unexpected error occurred";
    private static final String FILE_PROCESSING_ERROR = "An error occurred while processing your file";
    private static final String VALIDATION_FAILED = "Validation failed";
    private static final String USER_ALREADY_EXISTS = "User already exists";
    private static final String AUTHENTICATION_ERROR = "An error occurred during authentication";
    private static final String DOCUMENT_SAVE_ERROR = "An error occurred while uploading your statement";
    private static final String DOCUMENT_NOT_FOUND = "Document not found";

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

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        log.error(e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .message(VALIDATION_FAILED)
                        .description("Incorrect username or password")
                        .build());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        log.error(e.getMessage(), e);

        return ResponseEntity.internalServerError().body(
                ErrorResponse.builder()
                        .message(AUTHENTICATION_ERROR)
                        .description(e.getMessage())
                        .build());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException e) {
        log.error(e.getMessage(), e);

        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .message(USER_ALREADY_EXISTS)
                        .description(e.getMessage())
                        .build());
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
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error(e.getMessage(), e);

        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .message(VALIDATION_FAILED)
                        .build()
        );
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException e) {
        log.error(e.getMessage(), e);

        return ResponseEntity.internalServerError().body(
                ErrorResponse.builder()
                        .message(FILE_PROCESSING_ERROR)
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
