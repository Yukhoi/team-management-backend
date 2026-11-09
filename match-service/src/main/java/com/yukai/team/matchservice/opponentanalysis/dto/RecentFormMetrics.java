package com.yukai.team.matchservice.opponentanalysis.dto;

import com.yukai.team.matchservice.opponentanalysis.service.CacheStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Available official league/cup recent form across competitions and seasons; streaks cover only the eligible available sample")
public record RecentFormMetrics(
        Status status,
        @Schema(description = "Eligible scored matches before the target's Paris calendar date; null when the source or cutoff is unavailable")
        Integer sampleSize,
        @Schema(description = "Latest first, using existing won/draw/lost encoding")
        List<String> results,
        Window recentFive,
        Window recentTen,
        Source source,
        @Schema(description = "ALL_COMPETITIONS currently includes only LEAGUE and CUP, excluding unknown types")
        CompetitionScope competitionScope,
        @Schema(description = "Cross-season eligibility is allowed; the source page may provide only the current season, not complete history")
        SeasonScope seasonScope
) {
    public enum CompetitionScope { ALL_COMPETITIONS }
    public enum SeasonScope { CROSS_SEASON_ALLOWED }

    /** Source compatibility; missing scope on legacy saved metrics must remain unknown. */
    public RecentFormMetrics(Status status, Integer sampleSize, List<String> results, Window recentFive,
                             Window recentTen, Source source) {
        this(status, sampleSize, results, recentFive, recentTen, source, null, null);
    }

    // Historical validation statuses are retained for reading existing stored reports only.
    public enum Status {
        AVAILABLE, INSUFFICIENT_MATCHES, NO_COMPLETED_MATCHES,
        SOURCE_UNAVAILABLE, SEASON_UNVERIFIED, SEASON_MISMATCH,
        COMPETITION_UNVERIFIED, COMPETITION_MISMATCH, MATCH_TIME_UNAVAILABLE
    }

    public boolean available() {
        return status == Status.AVAILABLE || status == Status.INSUFFICIENT_MATCHES;
    }

    public static RecentFormMetrics unavailable(Status status, Source source) {
        return new RecentFormMetrics(status, null, List.of(), null, null, source,
                CompetitionScope.ALL_COMPETITIONS, SeasonScope.CROSS_SEASON_ALLOWED);
    }

    @Schema(description = "Actual window sample; winRate is null for zero matches")
    public record Window(int sampleSize, int wins, int draws, int losses, int goalsFor, int goalsAgainst,
                         BigDecimal winRate) { }

    @Schema(description = "Durable recent matches source; versionId is the canonical SHA-256, independent of standings")
    public record Source(Long flaTeamId, Long sourceSaisonId, String versionId, String payloadHash,
                         OffsetDateTime fetchedAt, CacheStatus cacheStatus, long cacheTtlSeconds,
                         boolean forceRefreshRequested, String errorCode) { }
}
