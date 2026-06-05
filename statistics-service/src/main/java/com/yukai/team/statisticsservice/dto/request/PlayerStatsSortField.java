package com.yukai.team.statisticsservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;

@Schema(description = "Supported player statistics sort field")
public enum PlayerStatsSortField {
    GOALS("goals"),
    ASSISTS("assists"),
    APPEARANCES("appearances"),
    STARTS("starts"),
    GOAL_INVOLVEMENTS("goalInvolvements");

    private final String property;

    PlayerStatsSortField(String property) {
        this.property = property;
    }

    public String getProperty() {
        return property;
    }

    public static PlayerStatsSortField from(String value) {
        return Arrays.stream(values())
                .filter(field -> field.property.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported sortBy: " + value));
    }
}
