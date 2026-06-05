package com.yukai.team.statisticsservice.controller;

import com.yukai.team.statisticsservice.dto.response.ApiResponse;
import com.yukai.team.statisticsservice.dto.response.DashboardStatisticsResponse;
import com.yukai.team.statisticsservice.dto.response.LeaderboardResponse;
import com.yukai.team.statisticsservice.dto.response.MatchSummaryResponse;
import com.yukai.team.statisticsservice.dto.response.PagedResponse;
import com.yukai.team.statisticsservice.dto.response.PlayerStatsResponse;
import com.yukai.team.statisticsservice.dto.response.TeamStatsResponse;
import com.yukai.team.statisticsservice.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.yukai.team.statisticsservice.service.query.DashboardStatisticsQueryService;
import com.yukai.team.statisticsservice.service.query.LeaderboardQueryService;
import com.yukai.team.statisticsservice.service.query.MatchStatisticsQueryService;
import com.yukai.team.statisticsservice.service.query.PlayerStatisticsQueryService;
import com.yukai.team.statisticsservice.service.query.TeamStatisticsQueryService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/statistics")
@Tag(name = "Statistics", description = "Statistics projection query APIs")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class StatisticsController {

    private final MatchStatisticsQueryService matchStatisticsQueryService;
    private final PlayerStatisticsQueryService playerStatisticsQueryService;
    private final LeaderboardQueryService leaderboardQueryService;
    private final TeamStatisticsQueryService teamStatisticsQueryService;
    private final DashboardStatisticsQueryService dashboardStatisticsQueryService;

    @GetMapping("/matches")
    @Operation(summary = "List match statistics", description = "List projected match summaries with pagination")
    public ApiResponse<PagedResponse<MatchSummaryResponse>> matches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long tournamentId
    ) {
        return ApiResponse.success(matchStatisticsQueryService.findMatches(page, size, tournamentId));
    }

    @GetMapping("/players")
    @Operation(summary = "List player statistics", description = "List projected player statistics with season, tournament, pagination and sorting parameters")
    public ApiResponse<PagedResponse<PlayerStatsResponse>> players(
            @RequestParam String season,
            @RequestParam Long tournamentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "goalInvolvements") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ApiResponse.success(playerStatisticsQueryService.findPlayers(
                season,
                tournamentId,
                page,
                size,
                sortBy,
                direction
        ));
    }

    @GetMapping("/leaderboards")
    @Operation(summary = "Get leaderboard", description = "Get a leaderboard for the selected metric, season and tournament")
    public ApiResponse<List<LeaderboardResponse>> leaderboards(
            @RequestParam String boardType,
            @RequestParam String season,
            @RequestParam Long tournamentId,
            @RequestParam(defaultValue = "20") int topN
    ) {
        return ApiResponse.success(leaderboardQueryService.findLeaderboard(boardType, season, tournamentId, topN));
    }

    @GetMapping("/teams")
    @Operation(summary = "List team standings", description = "List projected team standings for a season and tournament")
    public ApiResponse<List<TeamStatsResponse>> teams(
            @RequestParam String season,
            @RequestParam Long tournamentId
    ) {
        return ApiResponse.success(teamStatisticsQueryService.findTeams(season, tournamentId));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard", description = "Get the cached dashboard statistics summary")
    public ApiResponse<DashboardStatisticsResponse> dashboard() {
        return ApiResponse.success(dashboardStatisticsQueryService.getDashboard());
    }
}
