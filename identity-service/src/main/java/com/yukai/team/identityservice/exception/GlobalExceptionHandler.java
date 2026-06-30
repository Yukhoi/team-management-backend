package com.yukai.team.identityservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getMessage(), details));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
        HttpStatus status = switch (exception.getErrorCode()) {
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case USERNAME_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
                .body(error(exception.getErrorCode(), exception.getMessage(), List.of()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.getMessage(), List.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error(ErrorCode.VALIDATION_ERROR, exception.getMessage(), List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        log.error("Unhandled exception occurred", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage(), List.of()));
    }

    private ErrorResponse error(ErrorCode errorCode, String message, List<String> details) {
        return ErrorResponse.builder()
                .success(false)
                .errorCode(errorCode.getCode())
                .message(message)
                .details(details)
                .build();
    }
}
