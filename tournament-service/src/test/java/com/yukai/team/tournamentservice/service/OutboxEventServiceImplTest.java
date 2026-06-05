package com.yukai.team.tournamentservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yukai.team.tournamentservice.context.CurrentUser;
import com.yukai.team.tournamentservice.context.UserContextHolder;
import com.yukai.team.tournamentservice.entity.OutboxEvent;
import com.yukai.team.tournamentservice.outbox.OperatorProvider;
import com.yukai.team.tournamentservice.service.impl.OutboxEventServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboxEventServiceImplTest {

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void should_include_current_user_operator_in_event_payload() {
        CapturingEntityManager capturingEntityManager = new CapturingEntityManager();
        OutboxEventServiceImpl outboxEventService = outboxEventService(capturingEntityManager);
        UserContextHolder.set(new CurrentUser(1L, "admin", List.of("ADMIN", "COACH")));

        outboxEventService.saveEvent("tournament", 10L, "tournament.updated", Map.of("tournamentId", 10L));

        JsonNode payload = capturingEntityManager.persistedOutboxEvent.getPayloadJson();
        assertEquals(1L, payload.get("operator").get("userId").asLong());
        assertEquals("admin", payload.get("operator").get("username").asText());
        assertEquals(10L, payload.get("data").get("tournamentId").asLong());
    }

    @Test
    void should_include_system_operator_when_current_user_is_missing() {
        CapturingEntityManager capturingEntityManager = new CapturingEntityManager();
        OutboxEventServiceImpl outboxEventService = outboxEventService(capturingEntityManager);

        outboxEventService.saveEvent("tournament", 20L, "tournament.created", Map.of("tournamentId", 20L));

        JsonNode payload = capturingEntityManager.persistedOutboxEvent.getPayloadJson();
        assertTrue(payload.get("operator").get("userId").isNull());
        assertEquals("system", payload.get("operator").get("username").asText());
    }

    private OutboxEventServiceImpl outboxEventService(CapturingEntityManager capturingEntityManager) {
        return new OutboxEventServiceImpl(
                capturingEntityManager.entityManager(),
                objectMapper(),
                new OperatorProvider()
        );
    }

    private static ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    private static class CapturingEntityManager {

        private OutboxEvent persistedOutboxEvent;

        private EntityManager entityManager() {
            return (EntityManager) Proxy.newProxyInstance(
                    EntityManager.class.getClassLoader(),
                    new Class<?>[]{EntityManager.class},
                    (proxy, method, args) -> {
                        if ("persist".equals(method.getName())) {
                            persistedOutboxEvent = (OutboxEvent) args[0];
                        }
                        return null;
                    }
            );
        }
    }
}
