package com.yukai.team.matchservice.opponentanalysis.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ThreatLevel", description = "Opponent threat level", example = "HIGH")
public enum ThreatLevel {
    LOW,
    MEDIUM,
    HIGH
}
