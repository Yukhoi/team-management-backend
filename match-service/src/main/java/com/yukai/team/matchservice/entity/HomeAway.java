package com.yukai.team.matchservice.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Home or away designation")
public enum HomeAway {
    HOME,
    AWAY
}
