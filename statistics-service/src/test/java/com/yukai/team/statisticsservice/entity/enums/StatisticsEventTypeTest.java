package com.yukai.team.statisticsservice.entity.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatisticsEventTypeTest {

    @Test
    void mapsStringToEventType() {
        assertThat(StatisticsEventType.fromEventType("match.created"))
                .isEqualTo(StatisticsEventType.MATCH_CREATED);
        assertThat(StatisticsEventType.fromEventType("match.assist.deleted"))
                .isEqualTo(StatisticsEventType.MATCH_ASSIST_DELETED);
        assertThat(StatisticsEventType.fromEventType("match.assist.upserted"))
                .isEqualTo(StatisticsEventType.MATCH_ASSIST_UPSERTED);
        assertThat(StatisticsEventType.fromEventType("match.assist.created"))
                .isEqualTo(StatisticsEventType.MATCH_ASSIST_UPSERTED);
        assertThat(StatisticsEventType.fromEventType("match.assist.updated"))
                .isEqualTo(StatisticsEventType.MATCH_ASSIST_UPSERTED);
    }

    @Test
    void rejectsUnsupportedEventType() {
        assertThatThrownBy(() -> StatisticsEventType.fromEventType("team.created"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported eventType");
    }
}
