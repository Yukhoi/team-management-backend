package com.yukai.team.matchservice.opponentanalysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "LeaguePerformanceMetrics", description = "Deterministic league performance metrics")
public record LeaguePerformanceMetrics(
        @Schema(description = "Number of teams in the league table", example = "14")
        Integer teamCount,
        @Schema(description = "Approximate total matches represented by the standings", example = "182.00")
        BigDecimal totalMatchesApproximation,
        @Schema(description = "Average points", example = "32.10")
        BigDecimal averagePoints,
        @Schema(description = "Average points per game", example = "1.23")
        BigDecimal averagePointsPerGame,
        @Schema(description = "Average goals for", example = "48.20")
        BigDecimal averageGoalsFor,
        @Schema(description = "Average goals against", example = "48.20")
        BigDecimal averageGoalsAgainst,
        @Schema(description = "Average goals for per game", example = "1.85")
        BigDecimal averageGoalsForPerGame,
        @Schema(description = "Average goals against per game", example = "1.85")
        BigDecimal averageGoalsAgainstPerGame,
        @Schema(description = "Highest goals for in the league table", example = "112")
        Integer highestGoalsFor,
        @Schema(description = "Lowest goals against in the league table", example = "31")
        Integer lowestGoalsAgainst,
        @Schema(description = "Highest points in the league table", example = "68")
        Integer highestPoints,
        @Schema(description = "Average win rate", example = "0.38")
        BigDecimal averageWinRate,
        @Schema(description = "Average goal difference", example = "0.00")
        BigDecimal averageGoalDifference
) {
}
