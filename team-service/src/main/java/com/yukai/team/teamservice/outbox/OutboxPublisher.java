package com.yukai.team.teamservice.outbox;

import com.yukai.team.teamservice.entity.OutboxEvent;
import com.yukai.team.teamservice.entity.OutboxEventStatus;
import com.yukai.team.teamservice.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int MAX_RETRY = 10;
    private static final Collection<OutboxEventStatus> PUBLISHABLE_STATUSES = List.of(
            OutboxEventStatus.NEW,
            OutboxEventStatus.FAILED
    );

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxTopicRouter outboxTopicRouter;
    private final OutboxKafkaKeyBuilder outboxKafkaKeyBuilder;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            OutboxTopicRouter outboxTopicRouter,
            OutboxKafkaKeyBuilder outboxKafkaKeyBuilder,
            KafkaTemplate<String, String> kafkaTemplate,
            TransactionTemplate transactionTemplate
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxTopicRouter = outboxTopicRouter;
        this.outboxKafkaKeyBuilder = outboxKafkaKeyBuilder;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishNewEvents() {
        List<OutboxEvent> events = outboxEventRepository
                .findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(PUBLISHABLE_STATUSES, MAX_RETRY);
        for (OutboxEvent event : events) {
            try {
                processEvent(event);
            } catch (Exception ex) {
                log.error("Unexpected error while publishing outbox event, id={}", event.getId(), ex);
            }
        }
    }

    private void processEvent(OutboxEvent event) {
        Optional<PublishableEvent> optionalEvent = loadEvent(event.getId());
        if (optionalEvent.isEmpty()) {
            return;
        }

        PublishableEvent publishableEvent = optionalEvent.get();
        try {
            log.info("Publishing outbox event, id={}, topic={}, key={}",
                    publishableEvent.id(),
                    publishableEvent.topic(),
                    publishableEvent.key()
            );

            kafkaTemplate.send(publishableEvent.topic(), publishableEvent.key(), publishableEvent.payload()).get();

            markPublished(publishableEvent.id());
            log.info("Published outbox event, id={}, topic={}, key={}",
                    publishableEvent.id(),
                    publishableEvent.topic(),
                    publishableEvent.key()
            );
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            int retryCount = markFailed(publishableEvent.id());
            log.error("Failed to publish outbox event, id={}, topic={}, key={}, retryCount={}",
                    publishableEvent.id(),
                    publishableEvent.topic(),
                    publishableEvent.key(),
                    retryCount,
                    ex
            );
        }
    }

    private Optional<PublishableEvent> loadEvent(UUID eventId) {
        return transactionTemplate.execute(status -> {
            Optional<OutboxEvent> optionalEvent = outboxEventRepository.findById(eventId);
            if (optionalEvent.isEmpty()) {
                return Optional.empty();
            }

            OutboxEvent event = optionalEvent.get();
            if (!PUBLISHABLE_STATUSES.contains(event.getStatus()) || event.getRetryCount() >= MAX_RETRY) {
                return Optional.empty();
            }

            return Optional.of(new PublishableEvent(
                    event.getId(),
                    outboxTopicRouter.resolveTopic(event),
                    outboxKafkaKeyBuilder.buildKey(event),
                    event.getPayloadJson()
            ));
        });
    }

    private void markPublished(UUID eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            Optional<OutboxEvent> optionalEvent = outboxEventRepository.findById(eventId);
            if (optionalEvent.isEmpty()) {
                return;
            }

            OutboxEvent event = optionalEvent.get();
            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(OffsetDateTime.now());
            outboxEventRepository.save(event);
        });
    }

    private int markFailed(UUID eventId) {
        Integer retryCount = transactionTemplate.execute(status -> {
            Optional<OutboxEvent> optionalEvent = outboxEventRepository.findById(eventId);
            if (optionalEvent.isEmpty()) {
                return MAX_RETRY;
            }

            OutboxEvent event = optionalEvent.get();
            if (event.getRetryCount() >= MAX_RETRY) {
                return event.getRetryCount();
            }

            int nextRetryCount = event.getRetryCount() + 1;
            event.setRetryCount(nextRetryCount);
            event.setStatus(OutboxEventStatus.FAILED);
            outboxEventRepository.save(event);
            return nextRetryCount;
        });
        return retryCount == null ? MAX_RETRY : retryCount;
    }

    private record PublishableEvent(UUID id, String topic, String key, String payload) {
    }
}
