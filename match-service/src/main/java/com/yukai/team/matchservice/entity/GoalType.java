package com.yukai.team.matchservice.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Goal classification")
public enum GoalType {
    NORMAL,
    PENALTY,
    OWN_GOAL
}
