package com.yukai.team.statisticsservice.service.query;

import com.yukai.team.statisticsservice.dto.response.DashboardStatisticsResponse;
import com.yukai.team.statisticsservice.dto.response.LeaderboardResponse;
import com.yukai.team.statisticsservice.dto.response.TeamStatsResponse;
import com.yukai.team.statisticsservice.entity.enums.LeaderboardBoardType;
import com.yukai.team.statisticsservice.mapper.StatisticsResponseMapper;
import com.yukai.team.statisticsservice.repository.LeaderboardProjectionRepository;
import com.yukai.team.statisticsservice.repository.TeamStatsProjectionRepository;
import com.yukai.team.statisticsservice.service.cache.StatisticsCacheService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StatisticsQueryCacheTest {

    @Test
    void dashboardCacheHitSkipsDatabase() {
        TeamStatsProjectionRepository teamRepository = mock(TeamStatsProjectionRepository.class);
        LeaderboardProjectionRepository leaderboardRepository = mock(LeaderboardProjectionRepository.class);
        StatisticsCacheService cacheService = mock(StatisticsCacheService.class);
        DashboardStatisticsResponse cached = DashboardStatisticsResponse.builder().totalMatches(3).build();
        when(cacheService.get("statistics:dashboard", DashboardStatisticsResponse.class))
                .thenReturn(Optional.of(cached));

        DashboardStatisticsResponse response = new DashboardStatisticsQueryService(
                teamRepository,
                leaderboardRepository,
                cacheService
        ).getDashboard();

        assertThat(response).isSameAs(cached);
        verifyNoInteractions(teamRepository, leaderboardRepository);
        verify(cacheService, never()).put("statistics:dashboard", cached, Duration.ofSeconds(60));
    }

    @Test
    void dashboardCacheMissQueriesDatabaseAndCachesResponse() {
        TeamStatsProjectionRepository teamRepository = mock(TeamStatsProjectionRepository.class);
        LeaderboardProjectionRepository leaderboardRepository = mock(LeaderboardProjectionRepository.class);
        StatisticsCacheService cacheService = mock(StatisticsCacheService.class);
        when(cacheService.get("statistics:dashboard", DashboardStatisticsResponse.class))
                .thenReturn(Optional.empty());
        when(teamRepository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());

        DashboardStatisticsResponse response = new DashboardStatisticsQueryService(
                teamRepository,
                leaderboardRepository,
                cacheService
        ).getDashboard();

        assertThat(response.getTotalMatches()).isZero();
        verify(cacheService).put("statistics:dashboard", response, Duration.ofSeconds(60));
    }

    @Test
    void leaderboardCacheHitSkipsDatabase() {
        LeaderboardProjectionRepository repository = mock(LeaderboardProjectionRepository.class);
        StatisticsResponseMapper mapper = mock(StatisticsResponseMapper.class);
        StatisticsCacheService cacheService = mock(StatisticsCacheService.class);
        LeaderboardResponse cached = LeaderboardResponse.builder().entityId(1L).build();
        String key = "statistics:leaderboard:SCORER:2026:1:20";
        when(cacheService.get(key, LeaderboardResponse[].class))
                .thenReturn(Optional.of(new LeaderboardResponse[]{cached}));

        List<LeaderboardResponse> response =
                new LeaderboardQueryService(repository, mapper, cacheService)
                        .findLeaderboard("SCORER", "2026", 1L, 20);

        assertThat(response).containsExactly(cached);
        verifyNoInteractions(repository, mapper);
    }

    @Test
    void teamsCacheHitSkipsDatabase() {
        TeamStatsProjectionRepository repository = mock(TeamStatsProjectionRepository.class);
        StatisticsResponseMapper mapper = mock(StatisticsResponseMapper.class);
        StatisticsCacheService cacheService = mock(StatisticsCacheService.class);
        TeamStatsResponse cached = TeamStatsResponse.builder().teamId(1L).build();
        String key = "statistics:teams:2026:1";
        when(cacheService.get(key, TeamStatsResponse[].class))
                .thenReturn(Optional.of(new TeamStatsResponse[]{cached}));

        List<TeamStatsResponse> response =
                new TeamStatisticsQueryService(repository, mapper, cacheService)
                        .findTeams("2026", 1L);

        assertThat(response).containsExactly(cached);
        verifyNoInteractions(repository, mapper);
    }
}
