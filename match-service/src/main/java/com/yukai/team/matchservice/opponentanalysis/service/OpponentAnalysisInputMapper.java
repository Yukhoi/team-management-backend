package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.LeaguePerformanceMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.MatchOpponentMetricsResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamComparisonMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamPerformanceMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.OpponentAnalysisInput;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpponentAnalysisInputMapper {

    public static final String CONTRACT_VERSION = "v2";
    public static final String RECENT_FORM_RULES_VERSION = "v1-paris-date-league-cup-cross-season";

    private static final List<String> DATA_LIMITATIONS = List.of(
            "无球员级别数据",
            "无阵型数据",
            "无射门和控球数据",
            "无伤病和停赛数据",
            "无可靠历史交锋数据"
    );

    public OpponentAnalysisInput toInput(MatchOpponentMetricsResponse metrics) {
        return new OpponentAnalysisInput(
                new OpponentAnalysisInput.MatchContext(
                        metrics.match().tournamentName(),
                        metrics.match().season(),
                        metrics.match().matchTime() == null ? null : metrics.match().matchTime().withOffsetSameInstant(java.time.ZoneOffset.UTC),
                        metrics.match().homeAway()
                ),
                team(metrics.ourTeam(), metrics.match().ourTeamName(), "OUR_TEAM"),
                team(metrics.opponent(), metrics.match().opponentTeamName(), "OPPONENT"),
                league(metrics.league()),
                comparison(metrics),
                DATA_LIMITATIONS,
                new OpponentAnalysisInput.InputProvenance(CONTRACT_VERSION, RECENT_FORM_RULES_VERSION,
                        metrics.match().matchId(), metrics.dataSource().snapshotId(), metrics.dataSource().payloadHash(),
                        metrics.dataSource().provider(), metrics.dataSource().championnatId(), metrics.dataSource().saisonId(),
                        metrics.dataSource().formOrder(), metrics.dataSource().rankingType())
        );
    }

    private OpponentAnalysisInput.TeamMetrics team(TeamPerformanceMetrics team, String internalName, String role) {
        return new OpponentAnalysisInput.TeamMetrics(
                team.teamName(),
                team.calculatedRank(),
                team.points(),
                team.played(),
                team.wins(),
                team.draws(),
                team.losses(),
                team.goalsFor(),
                team.goalsAgainst(),
                team.goalsForPerGame(),
                team.goalsAgainstPerGame(),
                team.winRate(),
                team.recentFormAvailable() ? team.recentFive() : List.of(),
                new OpponentAnalysisInput.TeamIdentity(role, team.internalTeamId(), internalName, team.flaTeamId(), team.teamName()),
                recentForm(team)
        );
    }

    private OpponentAnalysisInput.RecentFormInput recentForm(TeamPerformanceMetrics team) {
        var form = team.recentForm();
        boolean available = team.recentFormAvailable();
        var source = form == null ? null : form.source();
        return new OpponentAnalysisInput.RecentFormInput(available,
                form == null ? "SOURCE_UNAVAILABLE" : form.status().name(), team.recentFormSampleSize(),
                available ? team.recentTen() : List.of(), available ? form.recentFive() : null,
                available ? form.recentTen() : null,
                available ? team.currentWinningStreak() : null, available ? team.currentDrawingStreak() : null,
                available ? team.currentLosingStreak() : null, available ? team.currentUnbeatenStreak() : null,
                available ? team.longestWinningStreak() : null, available ? team.longestLosingStreak() : null,
                form == null ? null : form.competitionScope(), form == null ? null : form.seasonScope(),
                source == null ? null : new OpponentAnalysisInput.RecentSource(source.flaTeamId(), source.sourceSaisonId(),
                        source.versionId(), source.payloadHash(), source.errorCode()));
    }

    private OpponentAnalysisInput.LeagueAverage league(LeaguePerformanceMetrics league) {
        return new OpponentAnalysisInput.LeagueAverage(
                league.averageGoalsForPerGame(),
                league.averageGoalsAgainstPerGame(),
                league.averagePointsPerGame(),
                league.averageWinRate()
        );
    }

    private OpponentAnalysisInput.ComparisonMetrics comparison(MatchOpponentMetricsResponse metrics) {
        TeamComparisonMetrics comparison = metrics.comparison();
        return new OpponentAnalysisInput.ComparisonMetrics(
                comparison.pointsDifference(),
                comparison.rankDifference(),
                comparison.winRateDifference(),
                comparison.pointsPerGameDifference(),
                comparison.goalsForPerGameDifference(),
                comparison.goalsAgainstPerGameDifference(),
                comparison.goalDifferenceDifference(),
                metrics.ourTeam().recentFormAvailable() && metrics.opponent().recentFormAvailable()
                        ? comparison.recentFiveWinsDifference() : null,
                comparison.opponentAttackVsOurDefenseGap(),
                comparison.ourAttackVsOpponentDefenseGap()
        );
    }
}
