package com.yukai.team.statisticsservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
@Schema(description = "Statistics API error response")
public class ErrorResponse {

    @Schema(description = "Whether the request succeeded", example = "false")
    private boolean success;

    @Schema(description = "Error details keyed by field or message", example = "{\"message\":\"Invalid boardType\"}")
    private Map<String, String> data;
}
