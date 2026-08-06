package com.yukai.team.matchservice.opponentanalysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(name = "TeamPerformanceMetrics", description = "Deterministic team performance metrics")
public record TeamPerformanceMetrics(
        @Schema(description = "Internal team ID", example = "12")
        Long internalTeamId,
        @Schema(description = "Mapped FLA team ID", example = "6580")
        Long flaTeamId,
        @Schema(description = "Team name", example = "YEXIAO PARIS FC")
        String teamName,
        @Schema(description = "Calculated league rank", example = "12")
        Integer calculatedRank,
        @Schema(description = "League points", example = "20")
        Integer points,
        @Schema(description = "Matches played", example = "26")
        Integer played,
        @Schema(description = "Wins", example = "6")
        Integer wins,
        @Schema(description = "Draws", example = "3")
        Integer draws,
        @Schema(description = "Losses", example = "17")
        Integer losses,
        @Schema(description = "Win rate", example = "0.23")
        BigDecimal winRate,
        @Schema(description = "Draw rate", example = "0.12")
        BigDecimal drawRate,
        @Schema(description = "Loss rate", example = "0.65")
        BigDecimal lossRate,
        @Schema(description = "Points per game", example = "0.77")
        BigDecimal pointsPerGame,
        @Schema(description = "Goals for", example = "63")
        Integer goalsFor,
        @Schema(description = "Goals against", example = "134")
        Integer goalsAgainst,
        @Schema(description = "Goal difference", example = "-71")
        Integer goalDifference,
        @Schema(description = "Goals for per game", example = "2.42")
        BigDecimal goalsForPerGame,
        @Schema(description = "Goals against per game", example = "5.15")
        BigDecimal goalsAgainstPerGame,
        @Schema(description = "FLA bonus points", example = "0")
        Integer bonus,
        @Schema(description = "FLA forfeits", example = "0")
        Integer forfeits,
        @Schema(description = "Recent five match results from latest to oldest", example = "[\"won\",\"lost\",\"lost\",\"won\",\"draw\"]")
        List<String> recentFive,
        @Schema(description = "Recent ten match results from latest to oldest", example = "[\"won\",\"lost\",\"lost\",\"won\",\"draw\"]")
        List<String> recentTen,
        @Schema(description = "Current winning streak", example = "1")
        Integer currentWinningStreak,
        @Schema(description = "Current drawing streak", example = "0")
        Integer currentDrawingStreak,
        @Schema(description = "Current losing streak", example = "0")
        Integer currentLosingStreak,
        @Schema(description = "Current unbeaten streak", example = "1")
        Integer currentUnbeatenStreak,
        @Schema(description = "Longest winning streak in available data", example = "2")
        Integer longestWinningStreak,
        @Schema(description = "Longest losing streak in available data", example = "4")
        Integer longestLosingStreak,
        @Schema(description = "Wins in recent five matches", example = "2")
        Integer recentFiveWins,
        @Schema(description = "Draws in recent five matches", example = "1")
        Integer recentFiveDraws,
        @Schema(description = "Losses in recent five matches", example = "2")
        Integer recentFiveLosses,
        @Schema(description = "Wins in recent ten matches", example = "2")
        Integer recentTenWins,
        @Schema(description = "Draws in recent ten matches", example = "1")
        Integer recentTenDraws,
        @Schema(description = "Losses in recent ten matches", example = "2")
        Integer recentTenLosses
) {
}
