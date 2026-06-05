package com.yukai.team.tournamentservice.entity.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Outbox event publishing status")
public enum OutboxEventStatus {
    NEW,
    PUBLISHED,
    FAILED
}
