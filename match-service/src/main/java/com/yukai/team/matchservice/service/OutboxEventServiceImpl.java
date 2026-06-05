package com.yukai.team.matchservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.matchservice.entity.OutboxEvent;
import com.yukai.team.matchservice.entity.OutboxStatus;
import com.yukai.team.matchservice.outbox.DomainEvent;
import com.yukai.team.matchservice.outbox.OperatorProvider;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class OutboxEventServiceImpl implements OutboxEventService {

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final OperatorProvider operatorProvider;

    public OutboxEventServiceImpl(
            EntityManager entityManager,
            ObjectMapper objectMapper,
            OperatorProvider operatorProvider
    ) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.operatorProvider = operatorProvider;
    }

    @Override
    public void saveEvent(String aggregateType, Long aggregateId, String eventType, Object data) {
        UUID eventId = UUID.randomUUID();
        DomainEvent domainEvent = new DomainEvent(
                eventId,
                eventType,
                aggregateType,
                aggregateId,
                OffsetDateTime.now(),
                operatorProvider.currentOperator(),
                data
        );

        JsonNode payloadJson = objectMapper.valueToTree(domainEvent);

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(eventId);
        outboxEvent.setAggregateType(aggregateType);
        outboxEvent.setAggregateId(aggregateId);
        outboxEvent.setEventType(eventType);
        outboxEvent.setPayloadJson(payloadJson);
        outboxEvent.setStatus(OutboxStatus.NEW);
        outboxEvent.setRetryCount(0);

        entityManager.persist(outboxEvent);
    }
}
