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
@Schema(description = "Login response containing JWT access token and refresh token")
public class LoginResponse {

    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessToken;
    @Schema(description = "Refresh token", example = "b2f8d7f6-7d41-4bd0-9b6d-84b0e9d7a1b2", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
    @Builder.Default
    @Schema(description = "Token type", example = "Bearer", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tokenType = "Bearer";
    @Schema(description = "Access token lifetime in seconds", example = "1800", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long expiresIn;
}
