package com.yukai.team.teamservice.outbox;

import java.time.OffsetDateTime;
import java.util.UUID;

public class DomainEvent {

    private UUID eventId;
    private String eventType;
    private String aggregateType;
    private Long aggregateId;
    private OffsetDateTime occurredAt;
    private EventOperator operator;
    private Object data;

    public DomainEvent() {
    }

    public DomainEvent(
            UUID eventId,
            String eventType,
            String aggregateType,
            Long aggregateId,
            OffsetDateTime occurredAt,
            EventOperator operator,
            Object data
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.occurredAt = occurredAt;
        this.operator = operator;
        this.data = data;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Long aggregateId) {
        this.aggregateId = aggregateId;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public EventOperator getOperator() {
        return operator;
    }

    public void setOperator(EventOperator operator) {
        this.operator = operator;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
