package com.yukai.team.matchservice.service;

public interface OutboxEventService {

    void saveEvent(String aggregateType, Long aggregateId, String eventType, Object data);
}
