package com.yukai.team.matchservice.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Outbox event publishing status")
public enum OutboxStatus {
    NEW,
    PUBLISHED,
    FAILED
}
