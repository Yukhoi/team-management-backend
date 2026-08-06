package com.yukai.team.matchservice.opponentanalysis.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "AnalysisFinding", description = "AI analysis finding with supporting evidence")
public record AnalysisFinding(
        @Schema(description = "Finding title", example = "进攻火力强")
        String title,
        @Schema(description = "Chinese analysis text", example = "场均进球远高于联赛平均。")
        String analysis,
        @Schema(description = "Evidence used by the AI report", example = "[\"场均进球4.31\"]")
        List<String> evidence
) {
    public AnalysisFinding {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
