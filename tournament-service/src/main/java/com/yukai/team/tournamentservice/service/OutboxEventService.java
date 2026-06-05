package com.yukai.team.tournamentservice.service;

public interface OutboxEventService {

    void saveEvent(String aggregateType, Long aggregateId, String eventType, Object data);
}
