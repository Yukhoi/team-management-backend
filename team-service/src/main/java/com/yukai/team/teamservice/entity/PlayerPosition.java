package com.yukai.team.teamservice.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Player field position")
public enum PlayerPosition {
    GOALKEEPER,   // Goalkeeper
    DEFENDER,     // Defender
    MIDFIELDER,   // Midfielder
    FORWARD       // Forward
}
