package com.yukai.team.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Reset password request")
public class ResetPasswordRequest {

    @NotBlank
    @Size(min = 6, max = 100)
    @Schema(description = "New password", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
