package com.yukai.team.matchservice.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Match lifecycle status")
public enum MatchStatus {
    SCHEDULED,
    ONGOING,
    FINISHED,
    CANCELLED
}
