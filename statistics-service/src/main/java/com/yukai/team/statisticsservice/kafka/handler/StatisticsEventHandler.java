package com.yukai.team.statisticsservice.kafka.handler;

import com.yukai.team.statisticsservice.dto.event.StatisticsEventEnvelope;
import com.yukai.team.statisticsservice.entity.enums.StatisticsEventType;

public interface StatisticsEventHandler {

    StatisticsEventType supports();

    void handle(StatisticsEventEnvelope event);
}
