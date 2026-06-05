package com.yukai.team.statisticsservice.dto.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsEventEnvelopeTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void eventJsonCanBeDeserialized() throws Exception {
        String eventId = "649a0aed-6948-4bf4-ae5b-466904e95549";
        String json = """
                {
                  "eventId": "%s",
                  "eventType": "match.created",
                  "aggregateType": "match",
                  "aggregateId": 2,
                  "occurredAt": "2026-05-20T02:49:39.596261+02:00",
                  "data": {
                    "matchId": 2
                  },
                  "ignored": "value"
                }
                """.formatted(eventId);

        StatisticsEventEnvelope event = objectMapper.readValue(json, StatisticsEventEnvelope.class);

        assertThat(event.getEventId()).isEqualTo(UUID.fromString(eventId));
        assertThat(event.getEventType()).isEqualTo("match.created");
        assertThat(event.getAggregateType()).isEqualTo("match");
        assertThat(event.getAggregateId()).isEqualTo(2L);
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getData().get("matchId").asLong()).isEqualTo(2L);
    }
}
