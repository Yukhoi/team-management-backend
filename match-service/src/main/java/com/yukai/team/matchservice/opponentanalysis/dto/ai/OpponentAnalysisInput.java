package com.yukai.team.matchservice.opponentanalysis.dto.ai;

import com.yukai.team.matchservice.entity.HomeAway;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OpponentAnalysisInput(
        MatchContext match,
        TeamMetrics ourTeam,
        TeamMetrics opponent,
        LeagueAverage leagueAverage,
        ComparisonMetrics comparison,
        List<String> dataLimitations
) {
    public OpponentAnalysisInput {
        dataLimitations = dataLimitations == null ? List.of() : List.copyOf(dataLimitations);
    }

    public record MatchContext(
            String competition,
            String season,
            OffsetDateTime matchTime,
            HomeAway homeAway
    ) {
    }

    public record TeamMetrics(
            String name,
            Integer calculatedRank,
            Integer points,
            Integer played,
            Integer wins,
            Integer draws,
            Integer losses,
            Integer goalsFor,
            Integer goalsAgainst,
            BigDecimal goalsForPerGame,
            BigDecimal goalsAgainstPerGame,
            BigDecimal winRate,
            List<String> recentFive
    ) {
        public TeamMetrics {
            recentFive = recentFive == null ? List.of() : List.copyOf(recentFive);
        }
    }

    public record LeagueAverage(
            BigDecimal averageGoalsForPerGame,
            BigDecimal averageGoalsAgainstPerGame,
            BigDecimal averagePointsPerGame,
            BigDecimal averageWinRate
    ) {
    }

    public record ComparisonMetrics(
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
}
