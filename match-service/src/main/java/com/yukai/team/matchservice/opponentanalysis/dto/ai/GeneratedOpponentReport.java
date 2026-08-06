package com.yukai.team.matchservice.opponentanalysis.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "GeneratedOpponentReport", description = "Structured AI-generated opponent analysis report")
public record GeneratedOpponentReport(
        @Schema(description = "Overall opponent threat level", implementation = ThreatLevel.class, example = "HIGH")
        ThreatLevel threatLevel,
        @Schema(description = "Chinese executive summary", example = "对手整体实力明显强于我方，需要优先控制中场失误并压缩防守空间。")
        String summary,
        @Schema(description = "Metric-by-metric comparison between our team and the opponent")
        List<ComparisonItem> comparison,
        @Schema(description = "Opponent strengths")
        List<AnalysisFinding> strengths,
        @Schema(description = "Opponent weaknesses")
        List<AnalysisFinding> weaknesses,
        @Schema(description = "Actionable tactical recommendations", example = "[\"加强防守。\",\"减少中场失误。\"]")
        List<String> recommendations,
        @Schema(description = "Known data limitations", example = "[\"无球员伤病数据。\",\"无历史交锋数据。\"]")
        List<String> dataLimitations
) {
    public GeneratedOpponentReport {
        comparison = comparison == null ? List.of() : List.copyOf(comparison);
        strengths = strengths == null ? List.of() : List.copyOf(strengths);
        weaknesses = weaknesses == null ? List.of() : List.copyOf(weaknesses);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        dataLimitations = dataLimitations == null ? List.of() : List.copyOf(dataLimitations);
    }
}
