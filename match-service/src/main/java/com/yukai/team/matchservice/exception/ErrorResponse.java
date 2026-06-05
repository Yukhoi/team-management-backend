package com.yukai.team.matchservice.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@Schema(description = "Match API error response")
public class ErrorResponse {

    private String code;
    private String message;
    private OffsetDateTime timestamp;
}
