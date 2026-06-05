package com.yukai.team.statisticsservice.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsCacheInvalidationService {

    private final StatisticsCacheService statisticsCacheService;

    public void evictStatisticsReadCaches() {
        statisticsCacheService.delete(StatisticsCacheKeyBuilder.dashboard());
        statisticsCacheService.deleteByPrefix(StatisticsCacheKeyBuilder.LEADERBOARD_PREFIX);
        statisticsCacheService.deleteByPrefix(StatisticsCacheKeyBuilder.TEAMS_PREFIX);
        log.info("Cache Evicted: dashboard, leaderboard and teams statistics caches");
    }
}
