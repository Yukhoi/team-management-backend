package com.yukai.team.matchservice.opponentanalysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Deterministic league performance metrics")
public record LeaguePerformanceMetrics(
        Integer teamCount,
        BigDecimal totalMatchesApproximation,
        BigDecimal averagePoints,
        BigDecimal averagePointsPerGame,
        BigDecimal averageGoalsFor,
        BigDecimal averageGoalsAgainst,
        BigDecimal averageGoalsForPerGame,
        BigDecimal averageGoalsAgainstPerGame,
        Integer highestGoalsFor,
        Integer lowestGoalsAgainst,
        Integer highestPoints,
        BigDecimal averageWinRate,
        BigDecimal averageGoalDifference
) {
}
