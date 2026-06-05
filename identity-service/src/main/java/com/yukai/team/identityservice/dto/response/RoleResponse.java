package com.yukai.team.identityservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role response")
public class RoleResponse {

    @Schema(description = "Role code", example = "ROLE_ADMIN")
    private String code;
    @Schema(description = "Role display name", example = "Administrator")
    private String name;
}
