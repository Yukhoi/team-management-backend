package com.yukai.team.statisticsservice.mapper;

import com.yukai.team.statisticsservice.dto.response.LeaderboardResponse;
import com.yukai.team.statisticsservice.dto.response.MatchSummaryResponse;
import com.yukai.team.statisticsservice.dto.response.PlayerStatsResponse;
import com.yukai.team.statisticsservice.dto.response.TeamStatsResponse;
import com.yukai.team.statisticsservice.entity.LeaderboardProjection;
import com.yukai.team.statisticsservice.entity.MatchSummaryProjection;
import com.yukai.team.statisticsservice.entity.PlayerStatsProjection;
import com.yukai.team.statisticsservice.entity.TeamStatsProjection;
import org.springframework.stereotype.Component;

@Component
public class StatisticsResponseMapper {

    public MatchSummaryResponse toMatchSummaryResponse(MatchSummaryProjection projection) {
        return MatchSummaryResponse.builder()
                .matchId(projection.getMatchId())
                .tournamentId(projection.getTournamentId())
                .tournamentName(projection.getTournamentNameSnapshot())
                .season(projection.getSeasonSnapshot())
                .matchTime(projection.getMatchTime())
                .opponentTeamName(projection.getOpponentTeamNameSnapshot())
                .homeAway(projection.getHomeAway().name())
                .ourScore(projection.getOurScore())
                .opponentScore(projection.getOpponentScore())
                .matchStatus(projection.getMatchStatus().name())
                .finished(projection.getFinished())
                .build();
    }

    public PlayerStatsResponse toPlayerStatsResponse(PlayerStatsProjection projection) {
        return PlayerStatsResponse.builder()
                .playerId(projection.getPlayerId())
                .playerName(projection.getPlayerNameSnapshot())
                .season(projection.getSeason())
                .tournamentId(projection.getTournamentId())
                .tournamentName(projection.getTournamentNameSnapshot())
                .appearances(projection.getAppearances())
                .starts(projection.getStarts())
                .goals(projection.getGoals())
                .assists(projection.getAssists())
                .goalInvolvements(projection.getGoalInvolvements())
                .build();
    }

    public LeaderboardResponse toLeaderboardResponse(LeaderboardProjection projection) {
        return LeaderboardResponse.builder()
                .rankNo(projection.getRankNo())
                .entityId(projection.getEntityId())
                .entityName(projection.getEntityNameSnapshot())
                .metricValue(projection.getMetricValue())
                .boardType(projection.getBoardType())
                .season(projection.getSeason())
                .tournamentId(projection.getTournamentId())
                .build();
    }

    public TeamStatsResponse toTeamStatsResponse(TeamStatsProjection projection) {
        return TeamStatsResponse.builder()
                .rankNo(projection.getRankNo())
                .teamId(projection.getTeamId())
                .teamName(projection.getTeamNameSnapshot())
                .played(projection.getPlayed())
                .win(projection.getWin())
                .draw(projection.getDraw())
                .lose(projection.getLose())
                .goalsFor(projection.getGoalsFor())
                .goalsAgainst(projection.getGoalsAgainst())
                .goalDiff(projection.getGoalDiff())
                .points(projection.getPoints())
                .season(projection.getSeason())
                .tournamentId(projection.getTournamentId())
                .tournamentName(projection.getTournamentNameSnapshot())
                .build();
    }
}
