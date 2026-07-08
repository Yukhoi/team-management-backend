package com.yukai.team.statisticsservice.service.query;

import com.yukai.team.statisticsservice.dto.response.DashboardStatisticsResponse;
import com.yukai.team.statisticsservice.dto.response.LeaderboardResponse;
import com.yukai.team.statisticsservice.dto.response.TeamStatsResponse;
import com.yukai.team.statisticsservice.entity.LeaderboardProjection;
import com.yukai.team.statisticsservice.entity.PlayerStatsProjection;
import com.yukai.team.statisticsservice.entity.enums.LeaderboardBoardType;
import com.yukai.team.statisticsservice.mapper.StatisticsResponseMapper;
import com.yukai.team.statisticsservice.repository.LeaderboardProjectionRepository;
import com.yukai.team.statisticsservice.repository.PlayerStatsProjectionRepository;
import com.yukai.team.statisticsservice.repository.TeamStatsProjectionRepository;
import com.yukai.team.statisticsservice.service.cache.StatisticsCacheService;
import com.yukai.team.statisticsservice.service.projection.LeaderboardProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsQueryCacheTest {

    @Mock
    private PlayerStatsProjectionRepository playerStatsProjectionRepository;

    @Mock
    private LeaderboardProjectionRepository leaderboardProjectionRepository;

    private LeaderboardProjectionService service;

    @BeforeEach
    void setUp() {
        service = new LeaderboardProjectionService(playerStatsProjectionRepository, leaderboardProjectionRepository);
    }

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

    @Test
    void rebuildLeaderboards_shouldCopyAppearancesToEachLeaderboardEntry() {
        PlayerStatsProjection player = PlayerStatsProjection.builder()
                .playerId(10L)
                .playerNameSnapshot("Messi")
                .season("2025")
                .tournamentId(1L)
                .goals(5)
                .assists(3)
                .appearances(12)
                .goalInvolvements(8)
                .build();

        when(playerStatsProjectionRepository.findBySeasonAndTournamentId("2025", 1L))
                .thenReturn(List.of(player));

        ArgumentCaptor<List<LeaderboardProjection>> captor =
                ArgumentCaptor.forClass(List.class);

        service.rebuildLeaderboards("2025", 1L);

        verify(leaderboardProjectionRepository, times(4)).saveAll(captor.capture());

        List<LeaderboardProjection> scorerBoard = captor.getAllValues().get(0);

        assertThat(scorerBoard).hasSize(1);
        assertThat(scorerBoard.get(0).getBoardType()).isEqualTo(LeaderboardBoardType.SCORER);
        assertThat(scorerBoard.get(0).getEntityId()).isEqualTo(10L);
        assertThat(scorerBoard.get(0).getMetricValue()).isEqualTo(5);

        assertThat(scorerBoard.get(0).getAppearanceCount()).isEqualTo(12);
    }
}
