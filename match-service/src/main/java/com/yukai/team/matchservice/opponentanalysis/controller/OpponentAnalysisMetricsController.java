package com.yukai.team.matchservice.opponentanalysis.controller;

import com.yukai.team.matchservice.context.CurrentUser;
import com.yukai.team.matchservice.context.UserContextHolder;
import com.yukai.team.matchservice.exception.ErrorResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.MatchOpponentMetricsResponse;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaAccessDeniedException;
import com.yukai.team.matchservice.opponentanalysis.service.MatchOpponentMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/matches/{matchId}/opponent-analysis")
@Tag(name = "Opponent Analysis", description = "AI opponent analysis generation, metrics and report query APIs")
@SecurityRequirement(name = "bearerAuth")
@Validated
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Match not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Opponent analysis context conflict", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "FLA service error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "504", description = "FLA request timeout", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class OpponentAnalysisMetricsController {

    private static final Set<String> READ_ROLES = Set.of("ADMIN", "COACH", "PLAYER");

    private final MatchOpponentMetricsService matchOpponentMetricsService;

    public OpponentAnalysisMetricsController(MatchOpponentMetricsService matchOpponentMetricsService) {
        this.matchOpponentMetricsService = matchOpponentMetricsService;
    }

    @GetMapping("/metrics")
    @Operation(
            summary = "Get deterministic opponent analysis metrics",
            description = "Fetches independent standings and team-match snapshots (separate default 6-hour TTLs). Recent form includes completed LEAGUE and CUP matches across leagues and seasons, without source season verification or league-name configuration. ALL_COMPETITIONS excludes unknown types; CROSS_SEASON_ALLOWED is an eligibility rule, not a guarantee of complete source history. Fewer than five eligible games is INSUFFICIENT_MATCHES but remains usable. All same-Paris-date games are excluded because final-whistle time is unavailable. Unavailable or empty form has null counts and streaks, empty result lists and an explicit status. forceRefresh=true refreshes both sources. Standings describe fetchedAt, not historical pre-match rank. This endpoint does not generate AI reports; existing report reuse still requires a composite source key."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Opponent analysis metrics returned",
            content = @Content(
                    schema = @Schema(implementation = MatchOpponentMetricsResponse.class),
                    examples = @ExampleObject(value = "{\"match\":{\"matchId\":42,\"tournamentId\":5,\"tournamentName\":\"Example internal league fixture\",\"season\":\"2026/2027\",\"ourTeamId\":12,\"ourTeamName\":\"YEXIAO PARIS FC\",\"opponentTeamId\":13,\"opponentTeamName\":\"18 EMPIRE\",\"matchTime\":\"2026-11-13T20:00:00Z\",\"homeAway\":\"HOME\",\"matchStatus\":\"SCHEDULED\"},\"ourTeam\":{\"internalTeamId\":12,\"flaTeamId\":270,\"teamName\":\"YEXIAO PARIS FC\",\"calculatedRank\":1,\"points\":20,\"played\":5,\"wins\":5,\"draws\":0,\"losses\":0,\"winRate\":1.0,\"drawRate\":0.0,\"lossRate\":0.0,\"pointsPerGame\":4.0,\"goalsFor\":29,\"goalsAgainst\":6,\"goalDifference\":23,\"goalsForPerGame\":5.8,\"goalsAgainstPerGame\":1.2,\"bonus\":0,\"forfeits\":0,\"recentFive\":[\"lost\",\"won\",\"won\",\"won\",\"won\"],\"recentTen\":[\"lost\",\"won\",\"won\",\"won\",\"won\",\"won\"],\"currentWinningStreak\":0,\"currentDrawingStreak\":0,\"currentLosingStreak\":1,\"currentUnbeatenStreak\":0,\"longestWinningStreak\":5,\"longestLosingStreak\":1,\"recentFiveWins\":4,\"recentFiveDraws\":0,\"recentFiveLosses\":1,\"recentTenWins\":5,\"recentTenDraws\":0,\"recentTenLosses\":1,\"recentForm\":{\"status\":\"AVAILABLE\",\"sampleSize\":6,\"results\":[\"lost\",\"won\",\"won\",\"won\",\"won\",\"won\"],\"recentFive\":{\"sampleSize\":5,\"wins\":4,\"draws\":0,\"losses\":1,\"goalsFor\":27,\"goalsAgainst\":8,\"winRate\":0.8},\"recentTen\":{\"sampleSize\":6,\"wins\":5,\"draws\":0,\"losses\":1,\"goalsFor\":31,\"goalsAgainst\":9,\"winRate\":0.83},\"source\":{\"flaTeamId\":270,\"sourceSaisonId\":null,\"versionId\":\"2c9c7ee937c71308b9834f6f38f1068542a3d74ae4b679263f2f64d8c127f98b\",\"payloadHash\":\"2c9c7ee937c71308b9834f6f38f1068542a3d74ae4b679263f2f64d8c127f98b\",\"fetchedAt\":\"2026-11-09T18:49:38.124385+01:00\",\"cacheStatus\":\"HIT\",\"cacheTtlSeconds\":21600,\"forceRefreshRequested\":false,\"errorCode\":null},\"competitionScope\":\"ALL_COMPETITIONS\",\"seasonScope\":\"CROSS_SEASON_ALLOWED\"},\"recentFormAvailable\":true,\"recentFormSampleSize\":6},\"opponent\":{\"internalTeamId\":13,\"flaTeamId\":63,\"teamName\":\"18 EMPIRE\",\"calculatedRank\":5,\"points\":6,\"played\":3,\"wins\":1,\"draws\":1,\"losses\":1,\"winRate\":0.33,\"drawRate\":0.33,\"lossRate\":0.33,\"pointsPerGame\":2.0,\"goalsFor\":8,\"goalsAgainst\":9,\"goalDifference\":-1,\"goalsForPerGame\":2.67,\"goalsAgainstPerGame\":3.0,\"bonus\":0,\"forfeits\":0,\"recentFive\":[\"won\",\"won\",\"draw\",\"lost\",\"lost\"],\"recentTen\":[\"won\",\"won\",\"draw\",\"lost\",\"lost\",\"draw\",\"draw\"],\"currentWinningStreak\":2,\"currentDrawingStreak\":0,\"currentLosingStreak\":0,\"currentUnbeatenStreak\":3,\"longestWinningStreak\":2,\"longestLosingStreak\":2,\"recentFiveWins\":2,\"recentFiveDraws\":1,\"recentFiveLosses\":2,\"recentTenWins\":2,\"recentTenDraws\":3,\"recentTenLosses\":2,\"recentForm\":{\"status\":\"AVAILABLE\",\"sampleSize\":7,\"results\":[\"won\",\"won\",\"draw\",\"lost\",\"lost\",\"draw\",\"draw\"],\"recentFive\":{\"sampleSize\":5,\"wins\":2,\"draws\":1,\"losses\":2,\"goalsFor\":13,\"goalsAgainst\":14,\"winRate\":0.4},\"recentTen\":{\"sampleSize\":7,\"wins\":2,\"draws\":3,\"losses\":2,\"goalsFor\":17,\"goalsAgainst\":18,\"winRate\":0.29},\"source\":{\"flaTeamId\":63,\"sourceSaisonId\":null,\"versionId\":\"86720ec9043361fcf35f2ffb5b7dafafb1be56531914942214894e57918ecf26\",\"payloadHash\":\"86720ec9043361fcf35f2ffb5b7dafafb1be56531914942214894e57918ecf26\",\"fetchedAt\":\"2026-11-09T18:49:38.153872+01:00\",\"cacheStatus\":\"HIT\",\"cacheTtlSeconds\":21600,\"forceRefreshRequested\":false,\"errorCode\":null},\"competitionScope\":\"ALL_COMPETITIONS\",\"seasonScope\":\"CROSS_SEASON_ALLOWED\"},\"recentFormAvailable\":true,\"recentFormSampleSize\":7},\"league\":{\"teamCount\":14,\"totalMatchesApproximation\":18.0,\"averagePoints\":6.29,\"averagePointsPerGame\":2.55,\"averageGoalsFor\":8.5,\"averageGoalsAgainst\":8.5,\"averageGoalsForPerGame\":3.66,\"averageGoalsAgainstPerGame\":2.99,\"highestGoalsFor\":29,\"lowestGoalsAgainst\":0,\"highestPoints\":20,\"averageWinRate\":0.51,\"averageGoalDifference\":0.0},\"comparison\":{\"pointsDifference\":14,\"rankDifference\":-4,\"winRateDifference\":0.67,\"pointsPerGameDifference\":2.0,\"goalsForPerGameDifference\":3.13,\"goalsAgainstPerGameDifference\":-1.8,\"goalDifferenceDifference\":24,\"recentFiveWinsDifference\":2,\"opponentAttackVsOurDefenseGap\":1.47,\"ourAttackVsOpponentDefenseGap\":2.8,\"recentFormComparisonAvailable\":true},\"dataSource\":{\"provider\":\"FLA\",\"championnatId\":16,\"saisonId\":2,\"snapshotId\":10,\"payloadHash\":\"7bdbaa154c3c94746faa9ca7557f7f2910fc95f941797117f316d7ac4099a156\",\"fetchedAt\":\"2026-11-03T00:00:00Z\",\"cacheStatus\":\"HIT\",\"cacheTtlSeconds\":21600,\"forceRefreshRequested\":false,\"formOrder\":\"LATEST_TO_OLDEST\",\"rankingType\":\"CALCULATED\",\"reportCacheCompatibility\":\"REQUIRES_COMPOSITE_SOURCE_KEY\",\"standingsTemporalScope\":\"FETCH_TIME_SNAPSHOT_NOT_HISTORICAL_PREMATCH\"}}")
            )
    )
    public MatchOpponentMetricsResponse getMetrics(
            @Parameter(description = "Match ID", example = "42") @PathVariable("matchId") @Positive Long matchId,
            @Parameter(description = "Force refresh FLA standings snapshot cache. true bypasses cache and refreshes FLA data only.", example = "false")
            @RequestParam(defaultValue = "false") Boolean forceRefresh
    ) {
        validatePositive(matchId, "matchId");
        requireRole();
        return matchOpponentMetricsService.getMetrics(matchId, Boolean.TRUE.equals(forceRefresh));
    }

    private void requireRole() {
        CurrentUser currentUser = UserContextHolder.get();
        if (currentUser == null || currentUser.roles() == null) {
            throw new FlaAccessDeniedException("ADMIN, COACH or PLAYER role is required to view opponent analysis metrics");
        }
        boolean allowed = currentUser.roles().stream().anyMatch(READ_ROLES::contains);
        if (!allowed) {
            throw new FlaAccessDeniedException("ADMIN, COACH or PLAYER role is required to view opponent analysis metrics");
        }
    }

    private void validatePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
