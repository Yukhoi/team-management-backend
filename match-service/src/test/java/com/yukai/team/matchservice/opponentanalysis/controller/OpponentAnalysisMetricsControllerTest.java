package com.yukai.team.matchservice.opponentanalysis.controller;

import com.yukai.team.matchservice.context.CurrentUser;
import com.yukai.team.matchservice.context.UserContextHolder;
import com.yukai.team.matchservice.entity.HomeAway;
import com.yukai.team.matchservice.entity.MatchStatus;
import com.yukai.team.matchservice.exception.GlobalExceptionHandler;
import com.yukai.team.matchservice.opponentanalysis.dto.LeaguePerformanceMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.MatchOpponentMetricsResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamComparisonMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamPerformanceMetrics;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaClientException;
import com.yukai.team.matchservice.opponentanalysis.exception.OpponentAnalysisConflictException;
import com.yukai.team.matchservice.opponentanalysis.service.CacheStatus;
import com.yukai.team.matchservice.opponentanalysis.service.MatchOpponentMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpponentAnalysisMetricsControllerTest {

    private MatchOpponentMetricsService matchOpponentMetricsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        matchOpponentMetricsService = mock(MatchOpponentMetricsService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OpponentAnalysisMetricsController(matchOpponentMetricsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void returnsMetrics() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "player", List.of("PLAYER")));
        when(matchOpponentMetricsService.getMetrics(42L, false)).thenReturn(response(false));

        mockMvc.perform(get("/api/v1/matches/42/opponent-analysis/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match.matchId").value(42))
                .andExpect(jsonPath("$.ourTeam.internalTeamId").value(12))
                .andExpect(jsonPath("$.opponent.internalTeamId").value(13))
                .andExpect(jsonPath("$.league.teamCount").value(2))
                .andExpect(jsonPath("$.comparison.pointsDifference").value(10))
                .andExpect(jsonPath("$.dataSource.provider").value("FLA"))
                .andExpect(jsonPath("$.dataSource.snapshotId").value(10))
                .andExpect(jsonPath("$.dataSource.payloadHash").value("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
                .andExpect(jsonPath("$.dataSource.cacheStatus").value("HIT"))
                .andExpect(jsonPath("$.dataSource.cacheTtlSeconds").value(21600))
                .andExpect(jsonPath("$.dataSource.forceRefreshRequested").value(false))
                .andExpect(jsonPath("$.dataSource.rankingType").value("CALCULATED"));
        verify(matchOpponentMetricsService).getMetrics(42L, false);
    }

    @Test
    void passesForceRefreshTrue() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "player", List.of("PLAYER")));
        when(matchOpponentMetricsService.getMetrics(42L, true)).thenReturn(response(true));

        mockMvc.perform(get("/api/v1/matches/42/opponent-analysis/metrics?forceRefresh=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataSource.forceRefreshRequested").value(true));
        verify(matchOpponentMetricsService).getMetrics(42L, true);
    }

    @Test
    void passesForceRefreshFalse() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "player", List.of("PLAYER")));
        when(matchOpponentMetricsService.getMetrics(42L, false)).thenReturn(response(false));

        mockMvc.perform(get("/api/v1/matches/42/opponent-analysis/metrics?forceRefresh=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataSource.forceRefreshRequested").value(false));
        verify(matchOpponentMetricsService).getMetrics(42L, false);
    }

    @Test
    void mapsNotFoundTo404() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        doThrow(new EntityNotFoundException("Match not found"))
                .when(matchOpponentMetricsService)
                .getMetrics(42L, false);

        mockMvc.perform(get("/api/v1/matches/42/opponent-analysis/metrics"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_FOUND"));
    }

    @Test
    void mapsConflictTo409() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        doThrow(new OpponentAnalysisConflictException("Our team FLA mapping is missing"))
                .when(matchOpponentMetricsService)
                .getMetrics(42L, false);

        mockMvc.perform(get("/api/v1/matches/42/opponent-analysis/metrics"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPPONENT_ANALYSIS_CONFLICT"));
    }

    @Test
    void mapsFlaClientExceptionTo502() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        doThrow(new FlaClientException(HttpStatus.BAD_GATEWAY, "FLA_SERVICE_UNAVAILABLE", "FLA service unavailable"))
                .when(matchOpponentMetricsService)
                .getMetrics(42L, false);

        mockMvc.perform(get("/api/v1/matches/42/opponent-analysis/metrics"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("FLA_SERVICE_UNAVAILABLE"));
    }

    @Test
    void mapsFlaTimeoutTo504() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        doThrow(new FlaClientException(HttpStatus.GATEWAY_TIMEOUT, "FLA_TIMEOUT", "FLA request timed out"))
                .when(matchOpponentMetricsService)
                .getMetrics(42L, false);

        mockMvc.perform(get("/api/v1/matches/42/opponent-analysis/metrics"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("FLA_TIMEOUT"));
    }

    @Test
    void rejectsInvalidMatchId() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));

        mockMvc.perform(get("/api/v1/matches/0/opponent-analysis/metrics"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsInvalidForceRefreshParameter() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));

        mockMvc.perform(get("/api/v1/matches/42/opponent-analysis/metrics?forceRefresh=not-boolean"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsMissingUserContext() throws Exception {
        mockMvc.perform(get("/api/v1/matches/42/opponent-analysis/metrics"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void rejectsUnsupportedRole() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "viewer", List.of("VIEWER")));

        mockMvc.perform(get("/api/v1/matches/42/opponent-analysis/metrics"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void exposesOpenApiContract() throws Exception {
        Tag tag = OpponentAnalysisMetricsController.class.getAnnotation(Tag.class);
        Method method = OpponentAnalysisMetricsController.class.getMethod("getMetrics", Long.class, Boolean.class);
        Operation operation = method.getAnnotation(Operation.class);
        Set<String> responseCodes = Arrays.stream(OpponentAnalysisMetricsController.class.getAnnotation(ApiResponses.class).value())
                .map(io.swagger.v3.oas.annotations.responses.ApiResponse::responseCode)
                .collect(Collectors.toSet());
        responseCodes.add(method.getAnnotation(io.swagger.v3.oas.annotations.responses.ApiResponse.class).responseCode());

        assertThat(tag.name()).isEqualTo("Opponent Analysis Metrics");
        assertThat(operation.summary()).contains("opponent analysis metrics");
        assertThat(responseCodes).contains("200", "400", "401", "403", "404", "409", "502", "504", "500");
    }

    private MatchOpponentMetricsResponse response(boolean forceRefreshRequested) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-05T12:00:00Z");
        return new MatchOpponentMetricsResponse(
                new MatchOpponentMetricsResponse.MatchContext(
                        42L,
                        5L,
                        "Paris League",
                        "2026",
                        12L,
                        "Our Team",
                        13L,
                        "Opponent Team",
                        OffsetDateTime.parse("2026-08-10T18:30:00Z"),
                        HomeAway.HOME,
                        MatchStatus.SCHEDULED
                ),
                teamMetrics(12L, 6580L, "Our FLA", 1, 20),
                teamMetrics(13L, 6581L, "Opponent FLA", 2, 10),
                new LeaguePerformanceMetrics(
                        2,
                        new BigDecimal("10.00"),
                        new BigDecimal("15.00"),
                        new BigDecimal("1.50"),
                        new BigDecimal("20.00"),
                        new BigDecimal("15.00"),
                        new BigDecimal("2.00"),
                        new BigDecimal("1.50"),
                        30,
                        10,
                        20,
                        new BigDecimal("0.45"),
                        new BigDecimal("5.00")
                ),
                new TeamComparisonMetrics(
                        10,
                        -1,
                        new BigDecimal("0.30"),
                        new BigDecimal("1.00"),
                        new BigDecimal("2.00"),
                        new BigDecimal("-1.00"),
                        30,
                        0,
                        new BigDecimal("0.00"),
                        new BigDecimal("1.00")
                ),
                new MatchOpponentMetricsResponse.DataSource(
                        "FLA",
                        1365L,
                        15L,
                        10L,
                        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                        now,
                        CacheStatus.HIT,
                        21600L,
                        forceRefreshRequested,
                        "LATEST_TO_OLDEST",
                        "CALCULATED"
                )
        );
    }

    private TeamPerformanceMetrics teamMetrics(
            Long internalTeamId,
            Long flaTeamId,
            String teamName,
            Integer calculatedRank,
            Integer points
    ) {
        return new TeamPerformanceMetrics(
                internalTeamId,
                flaTeamId,
                teamName,
                calculatedRank,
                points,
                10,
                6,
                2,
                2,
                new BigDecimal("0.60"),
                new BigDecimal("0.20"),
                new BigDecimal("0.20"),
                new BigDecimal("2.00"),
                30,
                10,
                20,
                new BigDecimal("3.00"),
                new BigDecimal("1.00"),
                0,
                0,
                List.of("won", "lost"),
                List.of("won", "lost"),
                1,
                0,
                0,
                1,
                1,
                1,
                1,
                0,
                1,
                1,
                0,
                1
        );
    }
}
