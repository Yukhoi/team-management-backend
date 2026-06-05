package com.yukai.team.auditservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.auditservice.dto.AuditEventMessage;
import com.yukai.team.auditservice.service.AuditEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final ObjectMapper objectMapper;
    private final AuditEventService auditEventService;
    private final DlqPublisher dlqPublisher;

    public AuditEventListener(
            ObjectMapper objectMapper,
            AuditEventService auditEventService,
            DlqPublisher dlqPublisher
    ) {
        this.objectMapper = objectMapper;
        this.auditEventService = auditEventService;
        this.dlqPublisher = dlqPublisher;
    }

    @KafkaListener(
            topics = {
                    "team-service.team-events",
                    "team-service.player-events",
                    "match-service.match-events",
                    "tournament-service.tournament-events"
            },
            groupId = "audit-service"
    )
    public void onMessage(
            String messageJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String sourceTopic
    ) {
        try {
            AuditEventMessage message = objectMapper.readValue(messageJson, AuditEventMessage.class);
            auditEventService.handleEvent(message);
        } catch (Exception ex) {
            log.error("Failed to process audit event, sending to DLQ: {}", messageJson, ex);
            try {
                dlqPublisher.publish(messageJson, ex, sourceTopic);
            } catch (Exception dlqEx) {
                log.error("Failed to publish audit event to DLQ", dlqEx);
            }
        }
    }
}
