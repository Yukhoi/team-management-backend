package com.yukai.team.statisticsservice.service.cache;

import com.yukai.team.statisticsservice.entity.enums.LeaderboardBoardType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsCacheKeyBuilderTest {

    @Test
    void buildsStatisticsReadCacheKeys() {
        assertThat(StatisticsCacheKeyBuilder.dashboard())
                .isEqualTo("statistics:dashboard");
        assertThat(StatisticsCacheKeyBuilder.leaderboard(
                LeaderboardBoardType.SCORER,
                "2026",
                1L,
                20
        )).isEqualTo("statistics:leaderboard:SCORER:2026:1:20");
        assertThat(StatisticsCacheKeyBuilder.teams("2026", 1L))
                .isEqualTo("statistics:teams:2026:1");
    }
}
