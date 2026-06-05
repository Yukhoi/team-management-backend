package com.yukai.team.statisticsservice.kafka.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.statisticsservice.dto.event.StatisticsEventEnvelope;
import com.yukai.team.statisticsservice.service.consumer.StatisticsEventProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchStatisticsKafkaListener {

    private final ObjectMapper objectMapper;
    private final StatisticsEventProcessingService statisticsEventProcessingService;

    @KafkaListener(
            topics = "${statistics.kafka.topic.match-events:match-service.match-events}",
            groupId = "${spring.kafka.consumer.group-id:statistics-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(String message, Acknowledgment acknowledgment) {
        StatisticsEventEnvelope event = deserialize(message);
        validateEvent(event);

        log.info("Received Kafka event: eventId={}, eventType={}", event.getEventId(), event.getEventType());

        boolean processed = statisticsEventProcessingService.process(event);
        acknowledgment.acknowledge();

        if (processed) {
            log.info("Event consumed successfully, eventId={}, eventType={}", event.getEventId(), event.getEventType());
        }
    }

    private StatisticsEventEnvelope deserialize(String message) {
        try {
            return objectMapper.readValue(message, StatisticsEventEnvelope.class);
        } catch (JsonProcessingException ex) {
            log.error("Failed to deserialize statistics event payload", ex);
            throw new IllegalArgumentException("Invalid statistics event payload", ex);
        }
    }

    private void validateEvent(StatisticsEventEnvelope event) {
        if (event.getEventId() == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
    }
}
