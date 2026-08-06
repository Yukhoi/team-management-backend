package com.yukai.team.matchservice.opponentanalysis.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "ComparisonItem", description = "AI comparison item for one metric")
public record ComparisonItem(
        @Schema(description = "Compared metric name", example = "联赛排名")
        String metric,
        @Schema(description = "Our team value", example = "12")
        String ourValue,
        @Schema(description = "Opponent value", example = "3")
        String opponentValue,
        @Schema(description = "Chinese comparison analysis", example = "对手排名明显更高。")
        String analysis
) {
}
