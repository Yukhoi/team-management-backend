package com.yukai.team.teamservice.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Player registration status")
public enum PlayerRegistrationStatus {
    REGISTERED,
    UNREGISTERED
}
