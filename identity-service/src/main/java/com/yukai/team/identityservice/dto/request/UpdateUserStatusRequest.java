package com.yukai.team.identityservice.dto.request;

import com.yukai.team.identityservice.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Update user status request")
public class UpdateUserStatusRequest {

    @NotNull
    @Schema(description = "New account status", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserStatus status;
}
