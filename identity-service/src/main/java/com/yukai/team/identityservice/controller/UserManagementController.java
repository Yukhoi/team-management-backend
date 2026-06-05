package com.yukai.team.identityservice.controller;

import com.yukai.team.identityservice.dto.request.CreateUserRequest;
import com.yukai.team.identityservice.dto.request.ResetPasswordRequest;
import com.yukai.team.identityservice.dto.request.UpdateUserRolesRequest;
import com.yukai.team.identityservice.dto.request.UpdateUserStatusRequest;
import com.yukai.team.identityservice.dto.response.PageResponse;
import com.yukai.team.identityservice.dto.response.UserManagementResponse;
import com.yukai.team.identityservice.service.UserManagementService;
import com.yukai.team.identityservice.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Administrator-only user management APIs")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Administrator role required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User or role not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username conflict", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class UserManagementController {

    private final UserManagementService userManagementService;

    @PostMapping
    @Operation(summary = "Create user", description = "Create a user account and assign roles")
    @ResponseStatus(HttpStatus.CREATED)
    public UserManagementResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userManagementService.createUser(request);
    }

    @GetMapping
    @Operation(summary = "List users", description = "List user accounts with pagination")
    public PageResponse<UserManagementResponse> getUsers(Pageable pageable) {
        return userManagementService.getUsers(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user", description = "Get a user account by ID")
    public UserManagementResponse getUser(@PathVariable Long id) {
        return userManagementService.getUser(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update user status", description = "Enable or disable a user account")
    public UserManagementResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return userManagementService.updateStatus(id, request);
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Update user roles", description = "Replace all roles assigned to a user account")
    public UserManagementResponse updateRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRolesRequest request
    ) {
        return userManagementService.updateRoles(id, request);
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset user password", description = "Reset a user's password and revoke existing refresh tokens")
    public UserManagementResponse resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        return userManagementService.resetPassword(id, request);
    }
}
