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
            description = "Fetches the match context, local FLA mappings and FLA standings snapshot, then returns Java-calculated metrics only. The FLA standings snapshot is cached for 6 hours by default. forceRefresh=true bypasses the cache and refreshes FLA data only; this stage does not generate AI reports."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Opponent analysis metrics returned",
            content = @Content(
                    schema = @Schema(implementation = MatchOpponentMetricsResponse.class),
                    examples = @ExampleObject(value = "{\"match\":{\"matchId\":42,\"tournamentId\":5,\"tournamentName\":\"Paris League\",\"season\":\"2026\",\"ourTeamId\":12,\"ourTeamName\":\"YEXIAO PARIS FC\",\"opponentTeamId\":13,\"opponentTeamName\":\"Opponent FC\",\"matchTime\":\"2026-08-10T18:30:00Z\",\"homeAway\":\"HOME\",\"matchStatus\":\"SCHEDULED\"},\"ourTeam\":{\"internalTeamId\":12,\"flaTeamId\":6580,\"teamName\":\"YEXIAO PARIS FC\",\"calculatedRank\":1,\"points\":46,\"played\":26,\"wins\":14,\"draws\":4,\"losses\":8,\"winRate\":0.54,\"drawRate\":0.15,\"lossRate\":0.31,\"pointsPerGame\":1.77,\"goalsFor\":63,\"goalsAgainst\":42,\"goalDifference\":21,\"goalsForPerGame\":2.42,\"goalsAgainstPerGame\":1.62,\"bonus\":0,\"forfeits\":0,\"recentFive\":[\"won\",\"draw\",\"lost\",\"won\",\"won\"],\"recentTen\":[\"won\",\"draw\",\"lost\",\"won\",\"won\"],\"currentWinningStreak\":1,\"currentDrawingStreak\":0,\"currentLosingStreak\":0,\"currentUnbeatenStreak\":2,\"longestWinningStreak\":2,\"longestLosingStreak\":1,\"recentFiveWins\":3,\"recentFiveDraws\":1,\"recentFiveLosses\":1,\"recentTenWins\":3,\"recentTenDraws\":1,\"recentTenLosses\":1},\"opponent\":{\"internalTeamId\":13,\"flaTeamId\":6581,\"teamName\":\"Opponent FC\",\"calculatedRank\":3,\"points\":38,\"played\":26,\"wins\":11,\"draws\":5,\"losses\":10,\"winRate\":0.42,\"drawRate\":0.19,\"lossRate\":0.38,\"pointsPerGame\":1.46,\"goalsFor\":50,\"goalsAgainst\":47,\"goalDifference\":3,\"goalsForPerGame\":1.92,\"goalsAgainstPerGame\":1.81,\"bonus\":0,\"forfeits\":0,\"recentFive\":[\"lost\",\"won\",\"draw\",\"won\",\"lost\"],\"recentTen\":[\"lost\",\"won\",\"draw\",\"won\",\"lost\"],\"currentWinningStreak\":0,\"currentDrawingStreak\":0,\"currentLosingStreak\":1,\"currentUnbeatenStreak\":0,\"longestWinningStreak\":1,\"longestLosingStreak\":1,\"recentFiveWins\":2,\"recentFiveDraws\":1,\"recentFiveLosses\":2,\"recentTenWins\":2,\"recentTenDraws\":1,\"recentTenLosses\":2},\"league\":{\"teamCount\":10,\"totalMatchesApproximation\":130.00,\"averagePoints\":32.10,\"averagePointsPerGame\":1.23,\"averageGoalsFor\":48.20,\"averageGoalsAgainst\":48.20,\"averageGoalsForPerGame\":1.85,\"averageGoalsAgainstPerGame\":1.85,\"highestGoalsFor\":72,\"lowestGoalsAgainst\":31,\"highestPoints\":54,\"averageWinRate\":0.38,\"averageGoalDifference\":0.00},\"comparison\":{\"pointsDifference\":8,\"rankDifference\":-2,\"winRateDifference\":0.12,\"pointsPerGameDifference\":0.31,\"goalsForPerGameDifference\":0.50,\"goalsAgainstPerGameDifference\":-0.19,\"goalDifferenceDifference\":18,\"recentFiveWinsDifference\":1,\"opponentAttackVsOurDefenseGap\":0.30,\"ourAttackVsOpponentDefenseGap\":0.61},\"dataSource\":{\"provider\":\"FLA\",\"championnatId\":1365,\"saisonId\":15,\"snapshotId\":10,\"payloadHash\":\"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef\",\"fetchedAt\":\"2026-08-05T12:00:00Z\",\"cacheStatus\":\"HIT\",\"cacheTtlSeconds\":21600,\"forceRefreshRequested\":false,\"formOrder\":\"LATEST_TO_OLDEST\",\"rankingType\":\"CALCULATED\"}}")
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
