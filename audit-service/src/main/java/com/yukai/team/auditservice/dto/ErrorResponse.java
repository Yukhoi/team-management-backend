package com.yukai.team.auditservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Audit API error response")
public class ErrorResponse {

    @Schema(description = "Error message", example = "Audit log not found")
    private String message;
}
