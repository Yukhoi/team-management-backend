package com.yukai.team.identityservice.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Identity service error code")
public enum ErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR", "Validation error"),
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Invalid credentials"),
    USER_DISABLED("USER_DISABLED", "User is disabled"),
    USER_LOCKED("USER_LOCKED", "User is locked"),
    USERNAME_ALREADY_EXISTS("USERNAME_ALREADY_EXISTS", "Username already exists"),
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Role not found"),
    INVALID_USER_STATUS("INVALID_USER_STATUS", "Invalid user status"),
    INVALID_REFRESH_TOKEN("INVALID_REFRESH_TOKEN", "Invalid refresh token"),
    REFRESH_TOKEN_EXPIRED("REFRESH_TOKEN_EXPIRED", "Refresh token expired"),
    ACCESS_DENIED("ACCESS_DENIED", "Access denied"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Internal server error");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
