package com.yukai.team.teamservice.outbox;

import com.yukai.team.teamservice.entity.OutboxEvent;
import org.springframework.stereotype.Component;

@Component
public class OutboxTopicRouter {

    private static final String TEAM_TOPIC = "team-service.team-events";
    private static final String PLAYER_TOPIC = "team-service.player-events";

    public String resolveTopic(OutboxEvent event) {
        if ("team".equals(event.getAggregateType())) {
            return TEAM_TOPIC;
        }
        if ("player".equals(event.getAggregateType())) {
            return PLAYER_TOPIC;
        }
        throw new IllegalArgumentException("Unsupported outbox aggregate type: " + event.getAggregateType());
    }
}
