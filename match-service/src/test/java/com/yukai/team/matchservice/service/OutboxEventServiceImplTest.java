package com.yukai.team.matchservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yukai.team.matchservice.context.CurrentUser;
import com.yukai.team.matchservice.context.UserContextHolder;
import com.yukai.team.matchservice.entity.OutboxEvent;
import com.yukai.team.matchservice.outbox.OperatorProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxEventServiceImplTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final OutboxEventServiceImpl outboxEventService = new OutboxEventServiceImpl(
            entityManager,
            objectMapper(),
            new OperatorProvider()
    );

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void should_include_current_user_operator_in_event_payload() {
        UserContextHolder.set(new CurrentUser(1L, "admin", List.of("ADMIN")));

        outboxEventService.saveEvent("match", 10L, "match.goal.created", Map.of("goalId", 20L));

        JsonNode payload = persistedPayload();
        assertEquals(1L, payload.get("operator").get("userId").asLong());
        assertEquals("admin", payload.get("operator").get("username").asText());
        assertEquals(20L, payload.get("data").get("goalId").asLong());
    }

    @Test
    void should_include_system_operator_when_current_user_is_missing() {
        outboxEventService.saveEvent("match", 10L, "match.created", Map.of("matchId", 10L));

        JsonNode payload = persistedPayload();
        assertEquals(true, payload.get("operator").get("userId").isNull());
        assertEquals("system", payload.get("operator").get("username").asText());
    }

    private JsonNode persistedPayload() {
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(entityManager).persist(captor.capture());
        return captor.getValue().getPayloadJson();
    }

    private static ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}
