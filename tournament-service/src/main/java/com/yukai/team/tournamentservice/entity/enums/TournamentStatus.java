package com.yukai.team.tournamentservice.entity.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tournament lifecycle status")
public enum TournamentStatus {
    ACTIVE,
    FINISHED,
    CANCELLED
}
