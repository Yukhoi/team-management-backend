package com.yukai.team.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create user request")
public class CreateUserRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    @Schema(description = "Username", example = "coach2", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    @Schema(description = "Initial password", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank
    @Size(min = 1, max = 100)
    @Schema(description = "Display name", example = "Coach Two", requiredMode = Schema.RequiredMode.REQUIRED)
    private String displayName;

    @Schema(description = "Role codes", example = "[\"COACH\"]")
    private Set<String> roles;

    @Schema(description = "Backward-compatible role code alias", example = "[\"COACH\"]")
    private Set<String> roleCodes;
}
