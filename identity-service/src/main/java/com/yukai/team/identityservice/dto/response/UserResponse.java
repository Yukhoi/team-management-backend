package com.yukai.team.identityservice.dto.response;

import com.yukai.team.identityservice.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User account response")
public class UserResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;
    @Schema(description = "Username", example = "coach")
    private String username;
    @Schema(description = "Display name", example = "Coach One")
    private String displayName;
    @Schema(description = "User status", example = "ACTIVE")
    private UserStatus status;
    @Schema(description = "Assigned roles")
    private List<RoleResponse> roles;
    @Schema(description = "Creation time", example = "2026-06-01T10:00:00Z")
    private OffsetDateTime createdAt;
}
