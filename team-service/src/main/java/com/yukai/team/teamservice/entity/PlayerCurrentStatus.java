package com.yukai.team.teamservice.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current player availability status")
public enum PlayerCurrentStatus {
    ACTIVE,
    INJURED,
    SUSPENDED,
    LEFT
}
