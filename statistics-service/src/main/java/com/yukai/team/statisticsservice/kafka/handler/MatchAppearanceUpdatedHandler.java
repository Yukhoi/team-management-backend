package com.yukai.team.statisticsservice.kafka.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.statisticsservice.dto.event.StatisticsEventEnvelope;
import com.yukai.team.statisticsservice.dto.event.data.MatchAppearanceUpdatedEventData;
import com.yukai.team.statisticsservice.entity.enums.StatisticsEventType;
import com.yukai.team.statisticsservice.service.projection.AppearanceProjectionService;
import com.yukai.team.statisticsservice.service.projection.LeaderboardProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchAppearanceUpdatedHandler implements StatisticsEventHandler {

    private final ObjectMapper objectMapper;
    private final AppearanceProjectionService appearanceProjectionService;
    private final LeaderboardProjectionService leaderboardProjectionService;

    @Override
    public StatisticsEventType supports() {
        return StatisticsEventType.MATCH_APPEARANCE_UPDATED;
    }

    @Override
    public void handle(StatisticsEventEnvelope event) {
        MatchAppearanceUpdatedEventData eventData =
                objectMapper.convertValue(event.getData(), MatchAppearanceUpdatedEventData.class);
        appearanceProjectionService.rebuildMatchAppearance(eventData);
        leaderboardProjectionService.rebuildLeaderboards(eventData.getSeasonSnapshot(), eventData.getTournamentId());
        log.info("match.appearance.updated processed, eventId={}, matchId={}", event.getEventId(), eventData.getMatchId());
    }
}
