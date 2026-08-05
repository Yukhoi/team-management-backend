package com.yukai.team.matchservice.opponentanalysis.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Opponent analysis report status")
public enum OpponentAnalysisStatus {
    PENDING,
    COMPLETED,
    FAILED
}
