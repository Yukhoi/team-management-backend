package com.yukai.team.identityservice.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Identity API error response")
public class ErrorResponse {

    @Builder.Default
    @Schema(description = "Whether the request succeeded", example = "false")
    private Boolean success = false;
    @Schema(description = "Machine-readable error code", example = "ACCESS_DENIED")
    private String errorCode;
    @Schema(description = "Human-readable error message", example = "Access denied")
    private String message;
    @Builder.Default
    @Schema(description = "Error timestamp")
    private OffsetDateTime timestamp = OffsetDateTime.now();
    @Schema(description = "Additional validation or business error details")
    private List<String> details;
}
