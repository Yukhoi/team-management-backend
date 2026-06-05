package com.yukai.team.statisticsservice.kafka.dispatcher;

import com.yukai.team.statisticsservice.dto.event.StatisticsEventEnvelope;
import com.yukai.team.statisticsservice.entity.enums.StatisticsEventType;
import com.yukai.team.statisticsservice.kafka.handler.StatisticsEventHandler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatisticsEventDispatcherTest {

    @Test
    void dispatchesToMatchingHandler() {
        AtomicBoolean handled = new AtomicBoolean(false);
        StatisticsEventHandler handler = new StatisticsEventHandler() {
            @Override
            public StatisticsEventType supports() {
                return StatisticsEventType.MATCH_CREATED;
            }

            @Override
            public void handle(StatisticsEventEnvelope event) {
                handled.set(true);
            }
        };
        StatisticsEventDispatcher dispatcher = new StatisticsEventDispatcher(List.of(handler));

        dispatcher.dispatch(StatisticsEventEnvelope.builder()
                .eventId(UUID.randomUUID())
                .eventType("match.created")
                .aggregateType("match")
                .aggregateId(1L)
                .build());

        assertThat(handled).isTrue();
    }

    @Test
    void unsupportedEventTypeThrows() {
        StatisticsEventDispatcher dispatcher = new StatisticsEventDispatcher(List.of());

        assertThatThrownBy(() -> dispatcher.dispatch(StatisticsEventEnvelope.builder()
                        .eventId(UUID.randomUUID())
                        .eventType("unknown.event")
                        .aggregateType("match")
                        .aggregateId(1L)
                        .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported eventType");
    }
}
