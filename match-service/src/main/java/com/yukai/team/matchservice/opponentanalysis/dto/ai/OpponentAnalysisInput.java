package com.yukai.team.matchservice.opponentanalysis.dto.ai;

import com.yukai.team.matchservice.entity.HomeAway;
import com.yukai.team.matchservice.opponentanalysis.dto.RecentFormMetrics;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OpponentAnalysisInput(
        MatchContext match,
        TeamMetrics ourTeam,
        TeamMetrics opponent,
        LeagueAverage leagueAverage,
        ComparisonMetrics comparison,
        List<String> dataLimitations,
        InputProvenance provenance
) {
    public OpponentAnalysisInput(MatchContext match, TeamMetrics ourTeam, TeamMetrics opponent,
                                 LeagueAverage leagueAverage, ComparisonMetrics comparison, List<String> dataLimitations) {
        this(match, ourTeam, opponent, leagueAverage, comparison, dataLimitations, null);
    }

    public record InputProvenance(String contractVersion, String recentFormRulesVersion, Long matchId,
                                  Long standingsSnapshotId, String standingsPayloadHash, String sourceProvider,
                                  Long championnatId, Long saisonId, String formOrder, String rankingType) { }

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
            List<String> recentFive,
            TeamIdentity identity,
            RecentFormInput recentForm
    ) {
        public TeamMetrics(String name, Integer calculatedRank, Integer points, Integer played, Integer wins,
                           Integer draws, Integer losses, Integer goalsFor, Integer goalsAgainst,
                           BigDecimal goalsForPerGame, BigDecimal goalsAgainstPerGame, BigDecimal winRate, List<String> recentFive) {
            this(name, calculatedRank, points, played, wins, draws, losses, goalsFor, goalsAgainst,
                    goalsForPerGame, goalsAgainstPerGame, winRate, recentFive, null, null);
        }

        public TeamMetrics {
            recentFive = recentFive == null ? List.of() : List.copyOf(recentFive);
        }
    }

    public record TeamIdentity(String role, Long internalTeamId, String internalTeamName,
                               Long flaTeamId, String flaTeamName) { }

    public record RecentFormInput(boolean recentFormAvailable, String recentFormStatus, Integer recentFormSampleSize,
                                  List<String> recentTen, RecentFormMetrics.Window recentFive,
                                  RecentFormMetrics.Window recentTenStatistics,
                                  Integer currentWinningStreak, Integer currentDrawingStreak,
                                  Integer currentLosingStreak, Integer currentUnbeatenStreak,
                                  Integer longestWinningStreak, Integer longestLosingStreak,
                                  RecentFormMetrics.CompetitionScope competitionScope,
                                  RecentFormMetrics.SeasonScope seasonScope,
                                  RecentSource source) { }

    public record RecentSource(Long flaTeamId, Long sourceSaisonId, String versionId, String payloadHash,
                               String errorCode) { }

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
        @com.fasterxml.jackson.annotation.JsonProperty
        public boolean recentFormComparisonAvailable() { return recentFiveWinsDifference != null; }
    }
}
