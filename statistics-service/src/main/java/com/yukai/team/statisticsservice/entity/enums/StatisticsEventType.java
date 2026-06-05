package com.yukai.team.statisticsservice.entity.enums;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;

@Schema(description = "Statistics projection event type")
public enum StatisticsEventType {
    MATCH_CREATED("match.created"),
    MATCH_RESULT_UPDATED("match.result.updated"),
    MATCH_APPEARANCE_UPDATED("match.appearance.updated"),
    MATCH_GOAL_CREATED("match.goal.created"),
    MATCH_GOAL_UPDATED("match.goal.updated"),
    MATCH_GOAL_DELETED("match.goal.deleted"),
    MATCH_ASSIST_UPSERTED("match.assist.upserted"),
    MATCH_ASSIST_DELETED("match.assist.deleted");

    private final String eventType;

    StatisticsEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventType() {
        return eventType;
    }

    public static StatisticsEventType fromEventType(String eventType) {
        if ("match.assist.created".equals(eventType) || "match.assist.updated".equals(eventType)) {
            return MATCH_ASSIST_UPSERTED;
        }
        return Arrays.stream(values())
                .filter(type -> type.eventType.equals(eventType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported eventType: " + eventType));
    }
}
