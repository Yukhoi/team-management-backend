package com.yukai.team.statisticsservice.service.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatisticsCacheInvalidationServiceTest {

    @Mock
    private StatisticsCacheService statisticsCacheService;

    @InjectMocks
    private StatisticsCacheInvalidationService statisticsCacheInvalidationService;

    @Test
    void evictsDashboardLeaderboardAndTeamsCaches() {
        statisticsCacheInvalidationService.evictStatisticsReadCaches();

        verify(statisticsCacheService).delete("statistics:dashboard");
        verify(statisticsCacheService).deleteByPrefix("statistics:leaderboard:");
        verify(statisticsCacheService).deleteByPrefix("statistics:teams:");
    }
}
