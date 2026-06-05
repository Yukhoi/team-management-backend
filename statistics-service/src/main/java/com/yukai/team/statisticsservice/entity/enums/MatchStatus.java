package com.yukai.team.statisticsservice.entity.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Projected match lifecycle status")
public enum MatchStatus {
    SCHEDULED,
    ONGOING,
    FINISHED,
    CANCELLED
}
