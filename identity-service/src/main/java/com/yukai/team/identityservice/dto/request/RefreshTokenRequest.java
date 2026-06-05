package com.yukai.team.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Refresh token request")
public class RefreshTokenRequest {

    @NotBlank
    @Schema(description = "Refresh token issued by login", example = "b2f8d7f6-7d41-4bd0-9b6d-84b0e9d7a1b2", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
