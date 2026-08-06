package com.yukai.team.matchservice.opponentanalysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "TeamComparisonMetrics", description = "Deterministic comparison metrics between our team and opponent")
public record TeamComparisonMetrics(
        @Schema(description = "Our points minus opponent points", example = "-48")
        Integer pointsDifference,
        @Schema(description = "Our rank minus opponent rank; positive means our rank number is lower in the table", example = "9")
        Integer rankDifference,
        @Schema(description = "Our win rate minus opponent win rate", example = "-0.62")
        BigDecimal winRateDifference,
        @Schema(description = "Our points per game minus opponent points per game", example = "-1.85")
        BigDecimal pointsPerGameDifference,
        @Schema(description = "Our goals-for per game minus opponent goals-for per game", example = "-1.89")
        BigDecimal goalsForPerGameDifference,
        @Schema(description = "Our goals-against per game minus opponent goals-against per game", example = "3.61")
        BigDecimal goalsAgainstPerGameDifference,
        @Schema(description = "Our goal difference minus opponent goal difference", example = "-143")
        Integer goalDifferenceDifference,
        @Schema(description = "Our recent five wins minus opponent recent five wins", example = "-2")
        Integer recentFiveWinsDifference,
        @Schema(description = "Opponent attack strength against our defensive concession rate", example = "0.88")
        BigDecimal opponentAttackVsOurDefenseGap,
        @Schema(description = "Our attack strength against opponent defensive concession rate", example = "-0.84")
        BigDecimal ourAttackVsOpponentDefenseGap
) {
}
