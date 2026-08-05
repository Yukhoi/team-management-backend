package com.yukai.team.matchservice.opponentanalysis.controller;

import com.yukai.team.matchservice.entity.HomeAway;
import com.yukai.team.matchservice.entity.MatchStatus;
import com.yukai.team.matchservice.opponentanalysis.dto.LeaguePerformanceMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.MatchOpponentMetricsResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamComparisonMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamPerformanceMetrics;
import com.yukai.team.matchservice.opponentanalysis.service.CacheStatus;
import com.yukai.team.matchservice.opponentanalysis.service.MatchOpponentMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:metrics-route-registration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE SCHEMA IF NOT EXISTS match",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=match",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "springdoc.api-docs.enabled=true"
})
class OpponentAnalysisMetricsRouteRegistrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private MatchOpponentMetricsService matchOpponentMetricsService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void metricsPathIsRegisteredInSpringMvc() throws Exception {
        when(matchOpponentMetricsService.getMetrics(8L, false)).thenReturn(response());

        mockMvc.perform(get("/api/v1/matches/8/opponent-analysis/metrics")
                        .header("X-User-Id", "1")
                        .header("X-Username", "coach")
                        .header("X-User-Roles", "COACH")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match.matchId").value(8))
                .andExpect(jsonPath("$.dataSource.cacheStatus").value("HIT"));
    }

    @Test
    void openApiDocsIncludeMetricsPath() throws Exception {
        mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/matches/{matchId}/opponent-analysis/metrics']").exists());
    }

    @Test
    void noResourceFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/matches/8/opponent-analysis/not-found")
                        .header("X-User-Id", "1")
                        .header("X-Username", "coach")
                        .header("X-User-Roles", "COACH")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private MatchOpponentMetricsResponse response() {
        return new MatchOpponentMetricsResponse(
                new MatchOpponentMetricsResponse.MatchContext(
                        8L,
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
                        OffsetDateTime.parse("2026-08-05T12:00:00Z"),
                        CacheStatus.HIT,
                        21600L,
                        false,
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
