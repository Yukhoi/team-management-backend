package com.yukai.team.statisticsservice.kafka.dispatcher;

import com.yukai.team.statisticsservice.dto.event.StatisticsEventEnvelope;
import com.yukai.team.statisticsservice.entity.enums.StatisticsEventType;
import com.yukai.team.statisticsservice.kafka.handler.StatisticsEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class StatisticsEventDispatcher {

    private final Map<StatisticsEventType, StatisticsEventHandler> handlers;

    public StatisticsEventDispatcher(List<StatisticsEventHandler> eventHandlers) {
        this.handlers = new EnumMap<>(StatisticsEventType.class);
        for (StatisticsEventHandler handler : eventHandlers) {
            StatisticsEventHandler previous = handlers.put(handler.supports(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate statistics event handler for " + handler.supports());
            }
        }
    }

    public void dispatch(StatisticsEventEnvelope event) {
        StatisticsEventType eventType;
        try {
            eventType = StatisticsEventType.fromEventType(event.getEventType());
        } catch (IllegalArgumentException ex) {
            log.warn("Unsupported statistics event type, eventId={}, eventType={}",
                    event.getEventId(),
                    event.getEventType());
            throw ex;
        }

        StatisticsEventHandler handler = handlers.get(eventType);
        if (handler == null) {
            log.warn("No statistics event handler registered, eventId={}, eventType={}",
                    event.getEventId(),
                    event.getEventType());
            throw new IllegalStateException("No statistics event handler registered for " + event.getEventType());
        }

        handler.handle(event);
    }
}
