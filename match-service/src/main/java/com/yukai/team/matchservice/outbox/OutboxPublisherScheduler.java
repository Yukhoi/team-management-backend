package com.yukai.team.matchservice.outbox;

import com.yukai.team.matchservice.entity.OutboxEvent;
import com.yukai.team.matchservice.entity.OutboxStatus;
import com.yukai.team.matchservice.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduler.class);
    private static final int MAX_RETRY_COUNT = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;
    private final String matchEventsTopic;

    public OutboxPublisherScheduler(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            TransactionTemplate transactionTemplate,
            @Value("${app.kafka.topics.match-events}") String matchEventsTopic
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionTemplate = transactionTemplate;
        this.matchEventsTopic = matchEventsTopic;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishNewEvents() {
        List<OutboxEvent> events = outboxEventRepository
                .findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
                        List.of(OutboxStatus.NEW, OutboxStatus.FAILED),
                        MAX_RETRY_COUNT
                );
        for (OutboxEvent event : events) {
            try {
                publish(event);
            } catch (Exception ex) {
                log.error("Unexpected error while publishing match outbox event, id={}", event.getId(), ex);
            }
        }
    }

    private void publish(OutboxEvent event) {
        String key = "match:" + event.getAggregateId();
        try {
            kafkaTemplate.send(matchEventsTopic, key, event.getPayloadJson().toString()).get();
            markPublished(event.getId());
            log.info("Published match outbox event, id={}, topic={}, key={}",
                    event.getId(),
                    matchEventsTopic,
                    key);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            int retryCount = markFailed(event.getId());
            log.error("Failed to publish match outbox event, id={}, topic={}, key={}, retryCount={}",
                    event.getId(),
                    matchEventsTopic,
                    key,
                    retryCount,
                    ex);
            if (retryCount >= MAX_RETRY_COUNT) {
                log.error("Max retry exceeded for outbox event, id={}, retryCount={}", event.getId(), retryCount);
            }
        }
    }

    private void markPublished(UUID eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            Optional<OutboxEvent> optionalEvent = outboxEventRepository.findById(eventId);
            if (optionalEvent.isEmpty()) {
                return;
            }
            OutboxEvent event = optionalEvent.get();
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(OffsetDateTime.now());
            outboxEventRepository.save(event);
        });
    }

    private int markFailed(UUID eventId) {
        Integer retryCount = transactionTemplate.execute(status -> {
            Optional<OutboxEvent> optionalEvent = outboxEventRepository.findById(eventId);
            if (optionalEvent.isEmpty()) {
                return 0;
            }
            OutboxEvent event = optionalEvent.get();
            int nextRetryCount = event.getRetryCount() + 1;
            event.setStatus(OutboxStatus.FAILED);
            event.setRetryCount(nextRetryCount);
            outboxEventRepository.save(event);
            return nextRetryCount;
        });
        return retryCount == null ? 0 : retryCount;
    }
}
