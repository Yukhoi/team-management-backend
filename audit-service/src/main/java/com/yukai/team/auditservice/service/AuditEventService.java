package com.yukai.team.auditservice.service;

import com.yukai.team.auditservice.dto.AuditEventMessage;
import com.yukai.team.auditservice.dto.EventOperator;
import com.yukai.team.auditservice.entity.OperationLog;
import com.yukai.team.auditservice.parser.AuditEventParser;
import com.yukai.team.auditservice.parser.AuditParseResult;
import com.yukai.team.auditservice.parser.DefaultAuditParser;
import com.yukai.team.auditservice.repository.EventConsumeRecordRepository;
import com.yukai.team.auditservice.repository.OperationLogRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditEventService {

    private static final String CONSUMER_NAME = "audit-service";
    private static final String SYSTEM_OPERATOR_USERNAME = "system";

    private final EventConsumeRecordRepository eventConsumeRecordRepository;
    private final OperationLogRepository operationLogRepository;
    private final List<AuditEventParser> parsers;
    private final DefaultAuditParser defaultAuditParser;

    public AuditEventService(
            EventConsumeRecordRepository eventConsumeRecordRepository,
            OperationLogRepository operationLogRepository,
            List<AuditEventParser> parsers,
            DefaultAuditParser defaultAuditParser
    ) {
        this.eventConsumeRecordRepository = eventConsumeRecordRepository;
        this.operationLogRepository = operationLogRepository;
        this.parsers = parsers;
        this.defaultAuditParser = defaultAuditParser;
    }

    @Transactional
    public void handleEvent(AuditEventMessage message) {
        validateMessage(message);

        try {
            int inserted = eventConsumeRecordRepository.insertIgnore(message.getEventId(), CONSUMER_NAME);
            if (inserted == 0) {
                return;
            }
        } catch (DataIntegrityViolationException ex) {
            return;
        }

        AuditEventParser parser = parsers.stream()
                .filter(p -> p.supports(message.getEventType()))
                .findFirst()
                .orElse(null);

        AuditParseResult result = parser != null
                ? parser.parse(message)
                : defaultAuditParser.parse(message);

        OperationLog operationLog = new OperationLog();
        operationLog.setEventId(message.getEventId());
        operationLog.setEventType(message.getEventType());
        operationLog.setBizType(message.getAggregateType());
        operationLog.setBizId(message.getAggregateId());
        EventOperator operator = message.getOperator();
        operationLog.setOperatorUserId(operator == null ? null : operator.getUserId());
        operationLog.setOperatorUsername(resolveOperatorUsername(operator));
        operationLog.setOperatorNameSnapshot(resolveOperatorUsername(operator));
        operationLog.setTraceId(null);
        operationLog.setBeforeData(result.getBeforeData());
        operationLog.setAfterData(result.getAfterData());
        operationLog.setOperatedAt(message.getOccurredAt());

        operationLogRepository.save(operationLog);
    }

    private String resolveOperatorUsername(EventOperator operator) {
        if (operator == null || operator.getUsername() == null || operator.getUsername().isBlank()) {
            return SYSTEM_OPERATOR_USERNAME;
        }
        return operator.getUsername();
    }

    private void validateMessage(AuditEventMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }

        if (message.getEventId() == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }

        if (message.getEventType() == null || message.getEventType().isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }

        if (message.getAggregateType() == null || message.getAggregateType().isBlank()) {
            throw new IllegalArgumentException("aggregateType must not be blank");
        }
    }
}
