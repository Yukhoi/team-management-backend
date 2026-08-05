package com.yukai.team.matchservice.opponentanalysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Deterministic comparison metrics between our team and opponent")
public record TeamComparisonMetrics(
        Integer pointsDifference,
        Integer rankDifference,
        BigDecimal winRateDifference,
        BigDecimal pointsPerGameDifference,
        BigDecimal goalsForPerGameDifference,
        BigDecimal goalsAgainstPerGameDifference,
        Integer goalDifferenceDifference,
        Integer recentFiveWinsDifference,
        BigDecimal opponentAttackVsOurDefenseGap,
        BigDecimal ourAttackVsOpponentDefenseGap
) {
}
