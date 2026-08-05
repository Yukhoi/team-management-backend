package com.yukai.team.matchservice.opponentanalysis.dto;

import com.yukai.team.matchservice.entity.HomeAway;
import com.yukai.team.matchservice.entity.MatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Match opponent analysis deterministic metrics response")
public record MatchOpponentMetricsResponse(
        MatchContext match,
        TeamPerformanceMetrics ourTeam,
        TeamPerformanceMetrics opponent,
        LeaguePerformanceMetrics league,
        TeamComparisonMetrics comparison,
        DataSource dataSource
) {

    @Schema(description = "Match context used for metrics")
    public record MatchContext(
            Long matchId,
            Long tournamentId,
            String tournamentName,
            String season,
            Long ourTeamId,
            String ourTeamName,
            Long opponentTeamId,
            String opponentTeamName,
            OffsetDateTime matchTime,
            HomeAway homeAway,
            MatchStatus matchStatus
    ) {
    }

    @Schema(description = "External data source metadata")
    public record DataSource(
            String provider,
            Long championnatId,
            Long saisonId,
            OffsetDateTime fetchedAt,
            String formOrder,
            String rankingType
    ) {
    }
}
