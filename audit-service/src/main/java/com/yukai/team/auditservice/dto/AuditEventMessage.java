package com.yukai.team.auditservice.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class AuditEventMessage {

    private UUID eventId;
    private String eventType;
    private String aggregateType;
    private Long aggregateId;
    private OffsetDateTime occurredAt;
    private EventOperator operator;
    private Object data;
}
