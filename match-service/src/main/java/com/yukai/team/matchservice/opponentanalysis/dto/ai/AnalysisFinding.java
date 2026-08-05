package com.yukai.team.matchservice.opponentanalysis.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnalysisFinding(
        String title,
        String analysis,
        List<String> evidence
) {
    public AnalysisFinding {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
