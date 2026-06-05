package com.yukai.team.tournamentservice.outbox;

import com.yukai.team.tournamentservice.entity.OutboxEvent;
import com.yukai.team.tournamentservice.entity.enums.OutboxEventStatus;
import com.yukai.team.tournamentservice.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final TournamentEventPublisher tournamentEventPublisher;
    private final TransactionTemplate transactionTemplate;

    public OutboxPublisherScheduler(
            OutboxEventRepository outboxEventRepository,
            TournamentEventPublisher tournamentEventPublisher,
            TransactionTemplate transactionTemplate
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.tournamentEventPublisher = tournamentEventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishOutboxEvents() {
        List<OutboxEvent> events = outboxEventRepository
                .findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
                        List.of(OutboxEventStatus.NEW, OutboxEventStatus.FAILED),
                        MAX_RETRY_COUNT
                );
        for (OutboxEvent event : events) {
            try {
                publish(event);
            } catch (Exception ex) {
                log.error("Unexpected error while publishing tournament outbox event, id={}", event.getId(), ex);
            }
        }
    }

    private void publish(OutboxEvent event) {
        String topic = tournamentEventPublisher.getTournamentEventsTopic();
        String key = event.getAggregateType() + ":" + event.getAggregateId();
        try {
            key = tournamentEventPublisher.publish(event);
            markPublished(event.getId());
            log.info("Published tournament outbox event, id={}, topic={}, key={}",
                    event.getId(),
                    topic,
                    key);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            int retryCount = markFailed(event.getId());
            log.error("Failed to publish tournament outbox event, id={}, topic={}, key={}, retryCount={}",
                    event.getId(),
                    topic,
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
            event.setStatus(OutboxEventStatus.PUBLISHED);
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
            event.setStatus(OutboxEventStatus.FAILED);
            event.setRetryCount(nextRetryCount);
            outboxEventRepository.save(event);
            return nextRetryCount;
        });
        return retryCount == null ? 0 : retryCount;
    }
}
