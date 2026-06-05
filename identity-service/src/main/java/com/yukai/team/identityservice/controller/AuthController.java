package com.yukai.team.identityservice.controller;

import com.yukai.team.identityservice.common.ApiResponse;
import com.yukai.team.identityservice.dto.request.ChangePasswordRequest;
import com.yukai.team.identityservice.dto.request.LoginRequest;
import com.yukai.team.identityservice.dto.request.LogoutRequest;
import com.yukai.team.identityservice.dto.request.RefreshTokenRequest;
import com.yukai.team.identityservice.dto.response.CurrentUserResponse;
import com.yukai.team.identityservice.dto.response.LoginResponse;
import com.yukai.team.identityservice.dto.response.TokenRefreshResponse;
import com.yukai.team.identityservice.exception.BusinessException;
import com.yukai.team.identityservice.exception.ErrorCode;
import com.yukai.team.identityservice.exception.ErrorResponse;
import com.yukai.team.identityservice.security.SecurityUtils;
import com.yukai.team.identityservice.service.AuthService;
import com.yukai.team.identityservice.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "Login, token lifecycle and current-user APIs")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with username and password and issue access and refresh tokens")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success("Login successful", response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Issue a new access token from a valid refresh token")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<TokenRefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return ApiResponse.success("Token refreshed successfully", response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoke a refresh token for the current user")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.success("Logout successful", null);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change the current user's password and revoke existing refresh tokens")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        authService.changePassword(currentUserId, request);
        return ApiResponse.success("Password changed successfully", null);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Return the authenticated user's profile and roles")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CurrentUserResponse> me() {
        return ApiResponse.success(currentUserService.getCurrentUser());
    }
}
