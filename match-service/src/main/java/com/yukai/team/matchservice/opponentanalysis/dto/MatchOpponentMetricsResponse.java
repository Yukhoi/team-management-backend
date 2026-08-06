package com.yukai.team.matchservice.opponentanalysis.dto;

import com.yukai.team.matchservice.entity.HomeAway;
import com.yukai.team.matchservice.entity.MatchStatus;
import com.yukai.team.matchservice.opponentanalysis.service.CacheStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(name = "OpponentAnalysisMetricsResponse", description = "Match opponent analysis deterministic metrics response")
public record MatchOpponentMetricsResponse(
        @Schema(description = "Match context used for metrics")
        MatchContext match,
        @Schema(description = "Our team deterministic performance metrics")
        TeamPerformanceMetrics ourTeam,
        @Schema(description = "Opponent deterministic performance metrics")
        TeamPerformanceMetrics opponent,
        @Schema(description = "League deterministic performance metrics")
        LeaguePerformanceMetrics league,
        @Schema(description = "Comparison metrics between our team and opponent")
        TeamComparisonMetrics comparison,
        @Schema(description = "External data source metadata")
        DataSource dataSource
) {

    @Schema(name = "MatchContext", description = "Match context used for metrics")
    public record MatchContext(
            @Schema(description = "Match ID", example = "42")
            Long matchId,
            @Schema(description = "Tournament ID", example = "5")
            Long tournamentId,
            @Schema(description = "Tournament name", example = "Paris League")
            String tournamentName,
            @Schema(description = "Season", example = "2026")
            String season,
            @Schema(description = "Our team ID", example = "12")
            Long ourTeamId,
            @Schema(description = "Our team name", example = "YEXIAO PARIS FC")
            String ourTeamName,
            @Schema(description = "Opponent team ID", example = "13")
            Long opponentTeamId,
            @Schema(description = "Opponent team name", example = "ASTERIA")
            String opponentTeamName,
            @Schema(description = "Scheduled match time", example = "2026-08-10T18:30:00Z")
            OffsetDateTime matchTime,
            @Schema(description = "Home or away flag", example = "HOME")
            HomeAway homeAway,
            @Schema(description = "Match status", example = "SCHEDULED")
            MatchStatus matchStatus
    ) {
    }

    @Schema(name = "DataSource", description = "External data source metadata")
    public record DataSource(
            @Schema(description = "External data provider", example = "FLA")
            String provider,
            @Schema(description = "FLA championnat ID", example = "1365")
            Long championnatId,
            @Schema(description = "FLA saison ID", example = "15")
            Long saisonId,
            @Schema(description = "External data snapshot ID", example = "23")
            Long snapshotId,
            @Schema(description = "SHA-256 hash of source payload", example = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
            String payloadHash,
            @Schema(description = "Source data fetch time", example = "2026-08-05T12:00:00Z")
            OffsetDateTime fetchedAt,
            @Schema(description = "Snapshot cache status", example = "HIT")
            CacheStatus cacheStatus,
            @Schema(description = "Cache TTL in seconds", example = "21600")
            Long cacheTtlSeconds,
            @Schema(description = "Whether cache bypass was requested", example = "false")
            Boolean forceRefreshRequested,
            @Schema(description = "Recent form ordering", example = "LATEST_TO_OLDEST")
            String formOrder,
            @Schema(description = "Ranking type", example = "CALCULATED")
            String rankingType
    ) {
    }
}
