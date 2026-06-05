package com.yukai.team.statisticsservice.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.statisticsservice.dto.response.DashboardStatisticsResponse;
import com.yukai.team.statisticsservice.dto.response.LeaderboardResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private StatisticsCacheService statisticsCacheService;

    @BeforeEach
    void setUp() {
        statisticsCacheService = new StatisticsCacheService(redisTemplate, new ObjectMapper());
    }

    @Test
    void readsAndConvertsJsonObject() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("statistics:dashboard")).thenReturn(Map.of(
                "totalMatches", 3,
                "wins", 2,
                "draws", 1,
                "losses", 0,
                "goals", 4,
                "goalsAgainst", 1,
                "topScorerGoals", 2,
                "topAssistCount", 1
        ));

        DashboardStatisticsResponse response = statisticsCacheService
                .get("statistics:dashboard", DashboardStatisticsResponse.class)
                .orElseThrow();

        assertThat(response.getTotalMatches()).isEqualTo(3);
        assertThat(response.getWins()).isEqualTo(2);
    }

    @Test
    void writesJsonValueWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        DashboardStatisticsResponse response = DashboardStatisticsResponse.builder()
                .totalMatches(3)
                .build();

        statisticsCacheService.put("statistics:dashboard", response, Duration.ofSeconds(60));

        verify(valueOperations).set("statistics:dashboard", response, Duration.ofSeconds(60));
    }

    @Test
    void readsAndConvertsJsonArray() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("statistics:leaderboard:SCORER:2026:1:20")).thenReturn(List.of(Map.of(
                "rankNo", 1,
                "entityId", 10,
                "entityName", "Player A",
                "metricValue", 3,
                "boardType", "SCORER",
                "season", "2026",
                "tournamentId", 1
        )));

        LeaderboardResponse[] response = statisticsCacheService
                .get("statistics:leaderboard:SCORER:2026:1:20", LeaderboardResponse[].class)
                .orElseThrow();

        assertThat(response).singleElement().satisfies(entry -> {
            assertThat(entry.getEntityId()).isEqualTo(10L);
            assertThat(entry.getMetricValue()).isEqualTo(3);
        });
    }
}
