package com.yukai.team.auditservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.auditservice.dto.DlqMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class DlqPublisher {

    private static final Logger log = LoggerFactory.getLogger(DlqPublisher.class);
    private static final String DLQ_TOPIC = "audit-service.DLQ";
    private static final String SOURCE_SERVICE = "audit-service";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DlqPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String originalMessage, Exception ex, String sourceTopic) {
        DlqMessage dlqMessage = new DlqMessage();
        dlqMessage.setOriginalMessage(originalMessage);
        dlqMessage.setErrorMessage(ex.getMessage());
        dlqMessage.setSourceTopic(sourceTopic);
        dlqMessage.setSourceService(SOURCE_SERVICE);
        dlqMessage.setFailedAt(OffsetDateTime.now());

        try {
            String dlqJson = objectMapper.writeValueAsString(dlqMessage);
            kafkaTemplate.send(DLQ_TOPIC, dlqJson);
        } catch (JsonProcessingException jsonEx) {
            log.error("Failed to serialize DLQ message for original message: {}", originalMessage, jsonEx);
        }
    }
}
