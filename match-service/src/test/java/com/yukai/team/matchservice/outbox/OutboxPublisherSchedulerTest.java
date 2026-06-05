package com.yukai.team.matchservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.matchservice.entity.OutboxEvent;
import com.yukai.team.matchservice.entity.OutboxStatus;
import com.yukai.team.matchservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OutboxPublisherSchedulerTest {

    private static final String TOPIC = "match-service.match-events";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_publish_new_and_failed_events_below_max_retry_count() {
        OutboxEvent event = buildEvent(OutboxStatus.FAILED, 2);
        FakeOutboxEventRepository repository = new FakeOutboxEventRepository(List.of(event));
        FakeKafkaTemplate kafkaTemplate = new FakeKafkaTemplate(false);
        OutboxPublisherScheduler scheduler = new OutboxPublisherScheduler(
                repository,
                kafkaTemplate,
                new ImmediateTransactionTemplate(),
                TOPIC
        );

        scheduler.publishNewEvents();

        assertEquals(List.of(OutboxStatus.NEW, OutboxStatus.FAILED), repository.requestedStatuses);
        assertEquals(5, repository.requestedMaxRetryCount);
        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());
        assertEquals(TOPIC, kafkaTemplate.topic);
        assertEquals("match:1", kafkaTemplate.key);
    }

    @Test
    void should_mark_failed_and_increment_retry_count_when_kafka_send_fails() {
        OutboxEvent event = buildEvent(OutboxStatus.NEW, 0);
        FakeOutboxEventRepository repository = new FakeOutboxEventRepository(List.of(event));
        OutboxPublisherScheduler scheduler = new OutboxPublisherScheduler(
                repository,
                new FakeKafkaTemplate(true),
                new ImmediateTransactionTemplate(),
                TOPIC
        );

        scheduler.publishNewEvents();

        assertEquals(OutboxStatus.FAILED, event.getStatus());
        assertEquals(1, event.getRetryCount());
    }

    @Test
    void should_stop_retrying_failed_events_when_retry_count_reaches_max() {
        OutboxEvent event = buildEvent(OutboxStatus.FAILED, 5);
        FakeOutboxEventRepository repository = new FakeOutboxEventRepository(List.of(event));
        FakeKafkaTemplate kafkaTemplate = new FakeKafkaTemplate(false);
        OutboxPublisherScheduler scheduler = new OutboxPublisherScheduler(
                repository,
                kafkaTemplate,
                new ImmediateTransactionTemplate(),
                TOPIC
        );

        scheduler.publishNewEvents();

        assertEquals(OutboxStatus.FAILED, event.getStatus());
        assertEquals(5, event.getRetryCount());
        assertEquals(null, kafkaTemplate.topic);
    }

    @Test
    void should_log_max_retry_after_failure_increments_retry_count_to_max() {
        OutboxEvent event = buildEvent(OutboxStatus.FAILED, 4);
        FakeOutboxEventRepository repository = new FakeOutboxEventRepository(List.of(event));
        OutboxPublisherScheduler scheduler = new OutboxPublisherScheduler(
                repository,
                new FakeKafkaTemplate(true),
                new ImmediateTransactionTemplate(),
                TOPIC
        );

        scheduler.publishNewEvents();

        assertEquals(OutboxStatus.FAILED, event.getStatus());
        assertEquals(5, event.getRetryCount());
    }

    private OutboxEvent buildEvent(OutboxStatus status, int retryCount) {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateType("match");
        event.setAggregateId(1L);
        event.setEventType("match.created");
        event.setPayloadJson(objectMapper.createObjectNode().put("matchId", 1L));
        event.setStatus(status);
        event.setRetryCount(retryCount);
        event.setCreatedAt(OffsetDateTime.now());
        return event;
    }

    private static class ImmediateTransactionTemplate extends TransactionTemplate {

        @Override
        public <T> T execute(TransactionCallback<T> action) {
            return action.doInTransaction(new SimpleTransactionStatus());
        }

        @Override
        public void executeWithoutResult(Consumer<TransactionStatus> action) {
            action.accept(new SimpleTransactionStatus());
        }
    }

    private static class FakeKafkaTemplate extends KafkaTemplate<String, String> {

        private final boolean fail;
        private String topic;
        private String key;

        FakeKafkaTemplate(boolean fail) {
            super(new DefaultKafkaProducerFactory<>(Map.of()));
            this.fail = fail;
        }

        @Override
        public CompletableFuture<SendResult<String, String>> send(String topic, String key, String data) {
            this.topic = topic;
            this.key = key;
            if (fail) {
                return CompletableFuture.failedFuture(new IllegalStateException("kafka unavailable"));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class FakeOutboxEventRepository implements OutboxEventRepository {

        private final List<OutboxEvent> events;
        private List<OutboxStatus> requestedStatuses;
        private Integer requestedMaxRetryCount;

        FakeOutboxEventRepository(List<OutboxEvent> events) {
            this.events = new ArrayList<>(events);
        }

        @Override
        public List<OutboxEvent> findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
                List<OutboxStatus> statuses,
                Integer maxRetryCount
        ) {
            this.requestedStatuses = statuses;
            this.requestedMaxRetryCount = maxRetryCount;
            return events.stream()
                    .filter(event -> statuses.contains(event.getStatus()))
                    .filter(event -> event.getRetryCount() < maxRetryCount)
                    .toList();
        }

        @Override
        public Optional<OutboxEvent> findById(UUID uuid) {
            return events.stream()
                    .filter(event -> event.getId().equals(uuid))
                    .findFirst();
        }

        @Override
        public <S extends OutboxEvent> S save(S entity) {
            return entity;
        }

        @Override
        public void flush() {
        }

        @Override
        public <S extends OutboxEvent> S saveAndFlush(S entity) {
            return entity;
        }

        @Override
        public <S extends OutboxEvent> List<S> saveAllAndFlush(Iterable<S> entities) {
            return saveAll(entities);
        }

        @Override
        public void deleteAllInBatch(Iterable<OutboxEvent> entities) {
        }

        @Override
        public void deleteAllByIdInBatch(Iterable<UUID> uuids) {
        }

        @Override
        public void deleteAllInBatch() {
        }

        @Override
        public OutboxEvent getOne(UUID uuid) {
            return findById(uuid).orElseThrow();
        }

        @Override
        public OutboxEvent getById(UUID uuid) {
            return findById(uuid).orElseThrow();
        }

        @Override
        public OutboxEvent getReferenceById(UUID uuid) {
            return findById(uuid).orElseThrow();
        }

        @Override
        public <S extends OutboxEvent> List<S> findAll(Example<S> example) {
            return List.of();
        }

        @Override
        public <S extends OutboxEvent> List<S> findAll(Example<S> example, Sort sort) {
            return List.of();
        }

        @Override
        public <S extends OutboxEvent> Optional<S> findOne(Example<S> example) {
            return Optional.empty();
        }

        @Override
        public <S extends OutboxEvent> Page<S> findAll(Example<S> example, Pageable pageable) {
            return Page.empty();
        }

        @Override
        public <S extends OutboxEvent> long count(Example<S> example) {
            return 0;
        }

        @Override
        public <S extends OutboxEvent> boolean exists(Example<S> example) {
            return false;
        }

        @Override
        public <S extends OutboxEvent, R> R findBy(
                Example<S> example,
                Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction
        ) {
            return null;
        }

        @Override
        public <S extends OutboxEvent> List<S> saveAll(Iterable<S> entities) {
            List<S> saved = new ArrayList<>();
            entities.forEach(saved::add);
            return saved;
        }

        @Override
        public List<OutboxEvent> findAll() {
            return events;
        }

        @Override
        public List<OutboxEvent> findAllById(Iterable<UUID> uuids) {
            List<OutboxEvent> found = new ArrayList<>();
            uuids.forEach(uuid -> findById(uuid).ifPresent(found::add));
            return found;
        }

        @Override
        public long count() {
            return events.size();
        }

        @Override
        public void deleteById(UUID uuid) {
        }

        @Override
        public void delete(OutboxEvent entity) {
        }

        @Override
        public void deleteAllById(Iterable<? extends UUID> uuids) {
        }

        @Override
        public void deleteAll(Iterable<? extends OutboxEvent> entities) {
        }

        @Override
        public void deleteAll() {
        }

        @Override
        public boolean existsById(UUID uuid) {
            return findById(uuid).isPresent();
        }

        @Override
        public List<OutboxEvent> findAll(Sort sort) {
            return events;
        }

        @Override
        public Page<OutboxEvent> findAll(Pageable pageable) {
            return Page.empty();
        }
    }
}
