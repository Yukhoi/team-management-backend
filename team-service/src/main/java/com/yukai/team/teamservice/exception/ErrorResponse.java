package com.yukai.team.teamservice.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Error response")
public class ErrorResponse {

    @Schema(description = "Error code", example = "VALIDATION_ERROR")
    private String code;

    @Schema(description = "Error message", example = "Request parameter validation failed")
    private String message;

    @Schema(description = "Error timestamp", example = "2026-05-02T00:00:00+02:00")
    private OffsetDateTime timestamp;

    @Schema(description = "Request path", example = "/api/players")
    private String path;

    @Schema(description = "Field-level error list")
    private List<FieldErrorResponse> errors;

    public ErrorResponse() {
    }

    public ErrorResponse(String code, String message, OffsetDateTime timestamp, String path) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
        this.path = path;
    }

    public ErrorResponse(String code, String message, OffsetDateTime timestamp, String path, List<FieldErrorResponse> errors) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
        this.path = path;
        this.errors = errors;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<FieldErrorResponse> getErrors() {
        return errors;
    }

    public void setErrors(List<FieldErrorResponse> errors) {
        this.errors = errors;
    }
}
