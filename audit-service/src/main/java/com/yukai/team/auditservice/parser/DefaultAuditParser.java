package com.yukai.team.auditservice.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.auditservice.dto.AuditEventMessage;
import org.springframework.stereotype.Component;

@Component
public class DefaultAuditParser {

    private final ObjectMapper objectMapper;

    public DefaultAuditParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
