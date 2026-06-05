package com.yukai.team.auditservice.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yukai.team.auditservice.dto.AuditEventMessage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class PlayerStatusChangedParser implements AuditEventParser {

    private static final String EVENT_TYPE = "player.status.changed";

    private final ObjectMapper objectMapper;

    public PlayerStatusChangedParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String eventType) {
        return EVENT_TYPE.equals(eventType);
    }

    @Override
    public AuditParseResult parse(AuditEventMessage message) {
        JsonNode data = objectMapper.valueToTree(message.getData());
        return new AuditParseResult(
                toStatusJson(data.get("oldStatus")),
                toStatusJson(data.get("newStatus"))
        );
    }

    private String toStatusJson(JsonNode statusNode) {
        ObjectNode node = objectMapper.createObjectNode();
        if (statusNode == null || statusNode.isNull()) {
            node.putNull("status");
        } else {
            node.set("status", statusNode);
        }
        return toJson(node);
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize audit event data", ex);
        }
    }
}
