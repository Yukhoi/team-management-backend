package com.yukai.team.matchservice.opponentanalysis.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedOpponentReport(
        ThreatLevel threatLevel,
        String summary,
        List<ComparisonItem> comparison,
        List<AnalysisFinding> strengths,
        List<AnalysisFinding> weaknesses,
        List<String> recommendations,
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
