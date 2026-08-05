package com.yukai.team.matchservice.opponentanalysis.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ComparisonItem(
        String metric,
        String ourValue,
        String opponentValue,
        String analysis
) {
}
