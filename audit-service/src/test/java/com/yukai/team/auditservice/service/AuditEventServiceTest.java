package com.yukai.team.auditservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yukai.team.auditservice.dto.AuditEventMessage;
import com.yukai.team.auditservice.dto.EventOperator;
import com.yukai.team.auditservice.entity.OperationLog;
import com.yukai.team.auditservice.parser.DefaultAuditParser;
import com.yukai.team.auditservice.repository.EventConsumeRecordRepository;
import com.yukai.team.auditservice.repository.OperationLogRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuditEventServiceTest {

    @Test
    void should_save_operator_from_event() {
        FakeRepositories repositories = new FakeRepositories(1);
        AuditEventService auditEventService = auditEventService(repositories);
        AuditEventMessage message = messageWithOperator(1L, "admin");

        auditEventService.handleEvent(message);

        OperationLog operationLog = repositories.savedOperationLog;
        assertEquals(1L, operationLog.getOperatorUserId());
        assertEquals("admin", operationLog.getOperatorUsername());
        assertEquals("admin", operationLog.getOperatorNameSnapshot());
        assertEquals("match.goal.created", operationLog.getEventType());
        assertEquals("match", operationLog.getBizType());
        assertEquals(1L, operationLog.getBizId());
    }

    @Test
    void should_save_system_operator_for_legacy_event_without_operator() {
        FakeRepositories repositories = new FakeRepositories(1);
        AuditEventService auditEventService = auditEventService(repositories);
        AuditEventMessage message = messageWithoutOperator();

        auditEventService.handleEvent(message);

        OperationLog operationLog = repositories.savedOperationLog;
        assertNull(operationLog.getOperatorUserId());
        assertEquals("system", operationLog.getOperatorUsername());
        assertEquals("system", operationLog.getOperatorNameSnapshot());
    }

    @Test
    void should_not_write_operation_log_for_duplicate_event() {
        FakeRepositories repositories = new FakeRepositories(0);
        AuditEventService auditEventService = auditEventService(repositories);
        AuditEventMessage message = messageWithOperator(1L, "admin");

        auditEventService.handleEvent(message);

        assertNull(repositories.savedOperationLog);
    }

    private AuditEventService auditEventService(FakeRepositories repositories) {
        return new AuditEventService(
                repositories.eventConsumeRecordRepository(),
                repositories.operationLogRepository(),
                List.of(),
                new DefaultAuditParser(objectMapper())
        );
    }

    private AuditEventMessage messageWithOperator(Long userId, String username) {
        AuditEventMessage message = messageWithoutOperator();
        EventOperator operator = new EventOperator();
        operator.setUserId(userId);
        operator.setUsername(username);
        message.setOperator(operator);
        return message;
    }

    private AuditEventMessage messageWithoutOperator() {
        AuditEventMessage message = new AuditEventMessage();
        message.setEventId(UUID.randomUUID());
        message.setEventType("match.goal.created");
        message.setAggregateType("match");
        message.setAggregateId(1L);
        message.setOccurredAt(OffsetDateTime.parse("2026-05-29T18:00:00+02:00"));
        message.setData(Map.of("goalId", 10L));
        return message;
    }

    private static ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    private static class FakeRepositories {

        private final int insertIgnoreResult;
        private OperationLog savedOperationLog;

        private FakeRepositories(int insertIgnoreResult) {
            this.insertIgnoreResult = insertIgnoreResult;
        }

        private EventConsumeRecordRepository eventConsumeRecordRepository() {
            return (EventConsumeRecordRepository) Proxy.newProxyInstance(
                    EventConsumeRecordRepository.class.getClassLoader(),
                    new Class<?>[]{EventConsumeRecordRepository.class},
                    (proxy, method, args) -> {
                        if ("insertIgnore".equals(method.getName())) {
                            return insertIgnoreResult;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private OperationLogRepository operationLogRepository() {
            return (OperationLogRepository) Proxy.newProxyInstance(
                    OperationLogRepository.class.getClassLoader(),
                    new Class<?>[]{OperationLogRepository.class},
                    (proxy, method, args) -> {
                        if ("save".equals(method.getName())) {
                            savedOperationLog = (OperationLog) args[0];
                            return savedOperationLog;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private static Object defaultValue(Class<?> returnType) {
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == long.class) {
                return 0L;
            }
            return null;
        }
    }
}
