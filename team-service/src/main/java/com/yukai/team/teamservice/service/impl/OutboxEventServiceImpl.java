package com.yukai.team.teamservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.teamservice.entity.OutboxEvent;
import com.yukai.team.teamservice.entity.OutboxEventStatus;
import com.yukai.team.teamservice.outbox.DomainEvent;
import com.yukai.team.teamservice.outbox.OperatorProvider;
import com.yukai.team.teamservice.service.OutboxEventService;
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
    public void saveEvent(String aggregateType, Long aggregateId, String eventType, Object payload) {
        UUID eventId = UUID.randomUUID();
        DomainEvent domainEvent = new DomainEvent(
                eventId,
                eventType,
                aggregateType,
                aggregateId,
                OffsetDateTime.now(),
                operatorProvider.currentOperator(),
                payload
        );

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(eventId);
        outboxEvent.setAggregateType(aggregateType);
        outboxEvent.setAggregateId(aggregateId);
        outboxEvent.setEventType(eventType);
        outboxEvent.setPayloadJson(toJson(domainEvent));
        outboxEvent.setStatus(OutboxEventStatus.NEW);
        outboxEvent.setRetryCount(0);

        entityManager.persist(outboxEvent);
    }

    private String toJson(DomainEvent domainEvent) {
        try {
            return objectMapper.writeValueAsString(domainEvent);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox event payload", ex);
        }
    }
}
