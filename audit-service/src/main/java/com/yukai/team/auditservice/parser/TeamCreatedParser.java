package com.yukai.team.auditservice.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.auditservice.dto.AuditEventMessage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class TeamCreatedParser implements AuditEventParser {

    private static final String EVENT_TYPE = "team.created";

    private final ObjectMapper objectMapper;

    public TeamCreatedParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String eventType) {
        return EVENT_TYPE.equals(eventType);
    }

    @Override
    public AuditParseResult parse(AuditEventMessage message) {
        return new AuditParseResult(null, toJson(message.getData()));
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize audit event data", ex);
        }
    }
}
