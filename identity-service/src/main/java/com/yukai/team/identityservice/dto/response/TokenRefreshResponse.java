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
@Schema(description = "Token refresh response")
public class TokenRefreshResponse {

    @Schema(description = "New JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessToken;
    @Builder.Default
    @Schema(description = "Token type", example = "Bearer", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tokenType = "Bearer";
    @Schema(description = "Access token lifetime in seconds", example = "1800", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long expiresIn;
}
