package com.yukai.team.identityservice.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User account status")
public enum UserStatus {
    ACTIVE,
    DISABLED,
    LOCKED
}
