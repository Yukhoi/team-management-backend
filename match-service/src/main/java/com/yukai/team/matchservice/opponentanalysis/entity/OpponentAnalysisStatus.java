package com.yukai.team.matchservice.opponentanalysis.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Status", description = "Opponent analysis report status", example = "COMPLETED")
public enum OpponentAnalysisStatus {
    PENDING,
    COMPLETED,
    FAILED
}
