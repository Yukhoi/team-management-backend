package com.yukai.team.statisticsservice.kafka.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.statisticsservice.dto.event.StatisticsEventEnvelope;
import com.yukai.team.statisticsservice.dto.event.data.MatchCreatedEventData;
import com.yukai.team.statisticsservice.entity.enums.StatisticsEventType;
import com.yukai.team.statisticsservice.service.projection.MatchSummaryProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchCreatedHandler implements StatisticsEventHandler {

    private final ObjectMapper objectMapper;
    private final MatchSummaryProjectionService matchSummaryProjectionService;

    @Override
    public StatisticsEventType supports() {
        return StatisticsEventType.MATCH_CREATED;
    }

    @Override
    public void handle(StatisticsEventEnvelope event) {
        MatchCreatedEventData eventData = objectMapper.convertValue(event.getData(), MatchCreatedEventData.class);
        matchSummaryProjectionService.upsertMatchCreated(eventData);
        log.info("match.created processed, eventId={}, matchId={}", event.getEventId(), eventData.getMatchId());
    }
}
