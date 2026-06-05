package com.yukai.team.statisticsservice.entity.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Leaderboard metric type")
public enum LeaderboardBoardType {
    SCORER,
    ASSIST,
    APPEARANCE,
    GOAL_INVOLVEMENT
}
