package com.yukai.team.statisticsservice.kafka.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.statisticsservice.dto.event.StatisticsEventEnvelope;
import com.yukai.team.statisticsservice.dto.event.data.MatchAssistUpsertedEventData;
import com.yukai.team.statisticsservice.entity.enums.StatisticsEventType;
import com.yukai.team.statisticsservice.service.projection.LeaderboardProjectionService;
import com.yukai.team.statisticsservice.service.projection.PlayerStatsProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchAssistUpsertedHandler implements StatisticsEventHandler {

    private final ObjectMapper objectMapper;
    private final PlayerStatsProjectionService playerStatsProjectionService;
    private final LeaderboardProjectionService leaderboardProjectionService;

    @Override
    public StatisticsEventType supports() {
        return StatisticsEventType.MATCH_ASSIST_UPSERTED;
    }

    @Override
    public void handle(StatisticsEventEnvelope event) {
        MatchAssistUpsertedEventData eventData = objectMapper.convertValue(event.getData(), MatchAssistUpsertedEventData.class);
        playerStatsProjectionService.applyAssistUpserted(eventData);
        leaderboardProjectionService.rebuildLeaderboards(eventData.getSeasonSnapshot(), eventData.getTournamentId());
        log.info("match.assist.upserted processed, eventId={}, assistId={}", event.getEventId(), eventData.getAssistId());
    }
}
