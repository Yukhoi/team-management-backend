package com.yukai.team.matchservice.outbox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {

    private UUID eventId;
    private String eventType;
    private String aggregateType;
    private Long aggregateId;
    private OffsetDateTime occurredAt;
    private EventOperator operator;
    private Object data;
}
