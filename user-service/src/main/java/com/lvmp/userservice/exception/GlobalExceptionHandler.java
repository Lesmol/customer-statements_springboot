package com.lvmp.userservice.exception;

import com.lvmp.userservice.model.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private static final String AN_ERROR_OCCURRED = "An unexpected error occurred";
    private static final String USER_NOT_FOUND = "User not found";
    private static final String FORBIDDEN = "You do not have permission to perform this action";

    @ExceptionHandler(UserDoesNotExist.class)
    public ResponseEntity<ErrorResponse> handleUserDoesNotExist(UserDoesNotExist e) {
        log.warn(e.getMessage());

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
