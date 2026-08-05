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
                        metrics.match().matchTime(),
                        metrics.match().homeAway()
                ),
                team(metrics.ourTeam()),
                team(metrics.opponent()),
                league(metrics.league()),
                comparison(metrics.comparison()),
                DATA_LIMITATIONS
        );
    }

    private OpponentAnalysisInput.TeamMetrics team(TeamPerformanceMetrics team) {
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
                team.recentFive()
        );
    }

    private OpponentAnalysisInput.LeagueAverage league(LeaguePerformanceMetrics league) {
        return new OpponentAnalysisInput.LeagueAverage(
                league.averageGoalsForPerGame(),
                league.averageGoalsAgainstPerGame(),
                league.averagePointsPerGame(),
                league.averageWinRate()
        );
    }

    private OpponentAnalysisInput.ComparisonMetrics comparison(TeamComparisonMetrics comparison) {
        return new OpponentAnalysisInput.ComparisonMetrics(
                comparison.pointsDifference(),
                comparison.rankDifference(),
                comparison.winRateDifference(),
                comparison.pointsPerGameDifference(),
                comparison.goalsForPerGameDifference(),
                comparison.goalsAgainstPerGameDifference(),
                comparison.goalDifferenceDifference(),
                comparison.recentFiveWinsDifference(),
                comparison.opponentAttackVsOurDefenseGap(),
                comparison.ourAttackVsOpponentDefenseGap()
        );
    }
}
