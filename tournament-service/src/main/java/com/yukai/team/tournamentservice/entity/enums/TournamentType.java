package com.yukai.team.tournamentservice.entity.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tournament format")
public enum TournamentType {
    LEAGUE,
    CUP,
    FRIENDLY
}
