package com.yukai.team.statisticsservice.kafka.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.statisticsservice.dto.event.StatisticsEventEnvelope;
import com.yukai.team.statisticsservice.service.consumer.StatisticsEventProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class MatchStatisticsKafkaListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final FakeStatisticsEventProcessingService processingService = new FakeStatisticsEventProcessingService();
    private final MatchStatisticsKafkaListener listener =
            new MatchStatisticsKafkaListener(objectMapper, processingService);

    @Test
    void duplicateEventIsAckedAndSkipped() {
        UUID eventId = UUID.randomUUID();
        TestAcknowledgment acknowledgment = new TestAcknowledgment();
        processingService.consumedEventIds.add(eventId);

        listener.onMessage(eventJson(eventId, "match.created"), acknowledgment);

        assertThat(processingService.processedEvents).isEmpty();
        assertThat(acknowledgment.acknowledged).isTrue();
    }

    @Test
    void normalEventIsDispatchedMarkedAndAcked() {
        UUID eventId = UUID.randomUUID();
        TestAcknowledgment acknowledgment = new TestAcknowledgment();

        listener.onMessage(eventJson(eventId, "match.created"), acknowledgment);

        assertThat(processingService.processedEvents).hasSize(1);
        assertThat(processingService.consumedEventIds).contains(eventId);
        assertThat(acknowledgment.acknowledged).isTrue();
    }

    @Test
    void missingEventIdThrowsAndDoesNotAck() {
        TestAcknowledgment acknowledgment = new TestAcknowledgment();
        String json = """
                {
                  "eventType": "match.created",
                  "aggregateType": "match",
                  "aggregateId": 1,
                  "occurredAt": "2026-05-20T02:49:39.596261+02:00",
                  "data": {}
                }
                """;

        assertThatThrownBy(() -> listener.onMessage(json, acknowledgment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventId");

        assertThat(acknowledgment.acknowledged).isFalse();
    }

    private String eventJson(UUID eventId, String eventType) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "aggregateType": "match",
                  "aggregateId": 1,
                  "occurredAt": "2026-05-20T02:49:39.596261+02:00",
                  "data": {}
                }
                """.formatted(eventId, eventType);
    }

    private static class FakeStatisticsEventProcessingService extends StatisticsEventProcessingService {

        private final List<UUID> consumedEventIds = new ArrayList<>();
        private final List<StatisticsEventEnvelope> processedEvents = new ArrayList<>();

        FakeStatisticsEventProcessingService() {
            super(null, null, null);
        }

        @Override
        public boolean process(StatisticsEventEnvelope event) {
            if (consumedEventIds.contains(event.getEventId())) {
                return false;
            }
            processedEvents.add(event);
            consumedEventIds.add(event.getEventId());
            return true;
        }
    }

    private static class TestAcknowledgment implements Acknowledgment {

        private boolean acknowledged;

        @Override
        public void acknowledge() {
            acknowledged = true;
        }
    }
}
