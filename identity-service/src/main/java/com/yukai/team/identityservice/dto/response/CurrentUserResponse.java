package com.yukai.team.identityservice.dto.response;

import com.yukai.team.identityservice.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Current authenticated user response")
public class CurrentUserResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;
    @Schema(description = "Username", example = "admin")
    private String username;
    @Schema(description = "Display name", example = "System Administrator")
    private String displayName;
    @Schema(description = "User status", example = "ACTIVE")
    private UserStatus status;
    @Schema(description = "Assigned roles")
    private List<RoleResponse> roles;
}
