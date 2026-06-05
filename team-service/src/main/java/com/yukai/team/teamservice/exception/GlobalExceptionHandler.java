package com.yukai.team.teamservice.exception;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Resource not found, path={}", request.getRequestURI(), ex);
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {
        log.warn("Business exception, path={}", request.getRequestURI(), ex);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "BUSINESS_ERROR",
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        log.warn("Invalid parameter, path={}", request.getRequestURI(), ex);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "Invalid request parameter type",
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        log.warn("Validation failed, path={}", request.getRequestURI(), ex);
        List<FieldErrorResponse> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                "Request parameter validation failed",
                OffsetDateTime.now(),
                request.getRequestURI(),
                errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("Invalid request body, path={}", request.getRequestURI(), ex);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST_BODY",
                "Request body is malformed or missing",
                request);
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        log.warn("Data integrity violation, path={}", request.getRequestURI(), ex);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "DATA_INTEGRITY_VIOLATION",
                resolveDataIntegrityViolationMessage(ex),
                request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleObjectOptimisticLockingFailureException(
            ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request) {
        log.warn("Concurrent modification, path={}", request.getRequestURI(), ex);
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "CONCURRENT_MODIFICATION",
                "The object has been modified by another user since it was last read. Please refresh the page and try again.",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            HttpServletRequest request) {
        log.error("Unhandled exception, path={}", request.getRequestURI(), ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Internal server error",
                request);
    }


    private String resolveDataIntegrityViolationMessage(DataIntegrityViolationException ex) {
        Throwable mostSpecificCause = ex.getMostSpecificCause();
        String message = mostSpecificCause == null ? null : mostSpecificCause.getMessage();
        if (message == null) {
            return "数据约束冲突，请检查是否存在重复数据";
        }
        if (message.contains("uk_team_name")) {
            return "球队名称已存在";
        }
        if (message.contains("uk_team_only_one_our_team")) {
            return "只能有一个我方球队";
        }
        if (message.contains("uk_player_active_jersey_number")) {
            return "同一球队中该球衣号码已被使用";
        }
        return "数据约束冲突，请检查是否存在重复数据";
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                code,
                message,
                OffsetDateTime.now(),
                request.getRequestURI());
        return ResponseEntity.status(status).body(errorResponse);
    }
}
