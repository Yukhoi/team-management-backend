package com.yukai.team.teamservice.outbox;

import com.yukai.team.teamservice.entity.OutboxEvent;
import org.springframework.stereotype.Component;

@Component
public class OutboxKafkaKeyBuilder {

    public String buildKey(OutboxEvent event) {
        if ("team".equals(event.getAggregateType())) {
            return "team:" + event.getAggregateId();
        }
        if ("player".equals(event.getAggregateType())) {
            return "player:" + event.getAggregateId();
        }
        throw new IllegalArgumentException("Unsupported outbox aggregate type: " + event.getAggregateType());
    }
}
