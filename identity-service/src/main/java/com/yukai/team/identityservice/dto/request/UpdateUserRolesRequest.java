package com.yukai.team.identityservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update user roles request")
public class UpdateUserRolesRequest {

    @Schema(description = "Replacement role codes", example = "[\"PLAYER\"]")
    private Set<String> roles;

    @Schema(description = "Backward-compatible replacement role code alias", example = "[\"PLAYER\"]")
    private Set<String> roleCodes;
}
