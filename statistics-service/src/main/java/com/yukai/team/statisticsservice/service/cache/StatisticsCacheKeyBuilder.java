package com.yukai.team.statisticsservice.service.cache;

import com.yukai.team.statisticsservice.entity.enums.LeaderboardBoardType;

public final class StatisticsCacheKeyBuilder {

    public static final String DASHBOARD_KEY = "statistics:dashboard";
    public static final String LEADERBOARD_PREFIX = "statistics:leaderboard:";
    public static final String TEAMS_PREFIX = "statistics:teams:";

    private StatisticsCacheKeyBuilder() {
    }

    public static String dashboard() {
        return DASHBOARD_KEY;
    }

    public static String leaderboard(
            LeaderboardBoardType boardType,
            String season,
            Long tournamentId,
            int topN
    ) {
        return LEADERBOARD_PREFIX + boardType.name() + ":" + season + ":" + tournamentId + ":" + topN;
    }

    public static String teams(String season, Long tournamentId) {
        return TEAMS_PREFIX + season + ":" + tournamentId;
    }
}
