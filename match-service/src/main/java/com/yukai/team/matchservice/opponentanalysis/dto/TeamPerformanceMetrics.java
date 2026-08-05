package com.yukai.team.matchservice.opponentanalysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Deterministic team performance metrics")
public record TeamPerformanceMetrics(
        Long internalTeamId,
        Long flaTeamId,
        String teamName,
        Integer calculatedRank,
        Integer points,
        Integer played,
        Integer wins,
        Integer draws,
        Integer losses,
        BigDecimal winRate,
        BigDecimal drawRate,
        BigDecimal lossRate,
        BigDecimal pointsPerGame,
        Integer goalsFor,
        Integer goalsAgainst,
        Integer goalDifference,
        BigDecimal goalsForPerGame,
        BigDecimal goalsAgainstPerGame,
        Integer bonus,
        Integer forfeits,
        List<String> recentFive,
        List<String> recentTen,
        Integer currentWinningStreak,
        Integer currentDrawingStreak,
        Integer currentLosingStreak,
        Integer currentUnbeatenStreak,
        Integer longestWinningStreak,
        Integer longestLosingStreak,
        Integer recentFiveWins,
        Integer recentFiveDraws,
        Integer recentFiveLosses,
        Integer recentTenWins,
        Integer recentTenDraws,
        Integer recentTenLosses
) {
}
