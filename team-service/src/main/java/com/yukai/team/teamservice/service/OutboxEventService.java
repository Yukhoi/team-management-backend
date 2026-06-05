package com.yukai.team.teamservice.service;

public interface OutboxEventService {

    void saveEvent(String aggregateType, Long aggregateId, String eventType, Object payload);
}
