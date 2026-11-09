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
        @Schema(description = "Recent five match results from latest to oldest; empty if unavailable (see recentForm)", example = "[\"won\",\"lost\",\"lost\",\"won\",\"draw\"]")
        List<String> recentFive,
        @Schema(description = "Recent ten match results from latest to oldest; empty if unavailable (see recentForm)", example = "[\"won\",\"lost\",\"lost\",\"won\",\"draw\"]")
        List<String> recentTen,
        @Schema(description = "Current winning streak; null if recent form unavailable", example = "1", nullable = true)
        Integer currentWinningStreak,
        @Schema(description = "Current drawing streak; null if recent form unavailable", example = "0", nullable = true)
        Integer currentDrawingStreak,
        @Schema(description = "Current losing streak; null if recent form unavailable", example = "0", nullable = true)
        Integer currentLosingStreak,
        @Schema(description = "Current unbeaten streak; null if recent form unavailable", example = "1", nullable = true)
        Integer currentUnbeatenStreak,
        @Schema(description = "Longest winning streak in available data; null if recent form unavailable", example = "2", nullable = true)
        Integer longestWinningStreak,
        @Schema(description = "Longest losing streak in available data; null if recent form unavailable", example = "4", nullable = true)
        Integer longestLosingStreak,
        @Schema(description = "Wins in recent five matches; null if recent form unavailable", example = "2", nullable = true)
        Integer recentFiveWins,
        @Schema(description = "Draws in recent five matches; null if recent form unavailable", example = "1", nullable = true)
        Integer recentFiveDraws,
        @Schema(description = "Losses in recent five matches; null if recent form unavailable", example = "2", nullable = true)
        Integer recentFiveLosses,
        @Schema(description = "Wins in recent ten matches; null if recent form unavailable", example = "2", nullable = true)
        Integer recentTenWins,
        @Schema(description = "Draws in recent ten matches; null if recent form unavailable", example = "1", nullable = true)
        Integer recentTenDraws,
        @Schema(description = "Losses in recent ten matches; null if recent form unavailable", example = "2", nullable = true)
        Integer recentTenLosses,
        @Schema(description = "Independent official-match recent form, availability and source; null for legacy stored reports")
        RecentFormMetrics recentForm
) {
    /** Source compatibility for existing callers and legacy test fixtures. */
    public TeamPerformanceMetrics(
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
            Integer recentTenLosses) {
        this(internalTeamId, flaTeamId, teamName, calculatedRank, points, played, wins, draws, losses, winRate, drawRate, lossRate, pointsPerGame, goalsFor, goalsAgainst, goalDifference, goalsForPerGame, goalsAgainstPerGame, bonus, forfeits, recentFive, recentTen, currentWinningStreak, currentDrawingStreak, currentLosingStreak, currentUnbeatenStreak, longestWinningStreak, longestLosingStreak, recentFiveWins, recentFiveDraws, recentFiveLosses, recentTenWins, recentTenDraws, recentTenLosses, null);
    }

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    @Schema(description = "True for a nonempty eligible completed LEAGUE/CUP sample")
    public boolean recentFormAvailable() {
        return recentForm != null && recentForm.available();
    }

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    @Schema(description = "Actual eligible sample size; null when source or cutoff is unavailable", nullable = true)
    public Integer recentFormSampleSize() {
        return recentForm == null ? null : recentForm.sampleSize();
    }
}
