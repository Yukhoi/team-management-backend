package com.yukai.team.teamservice.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Outbox event publishing status")
public enum OutboxEventStatus {
    NEW,
    PUBLISHED,
    FAILED
}
