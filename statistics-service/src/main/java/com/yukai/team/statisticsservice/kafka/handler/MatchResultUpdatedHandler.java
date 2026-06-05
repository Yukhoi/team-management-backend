package com.yukai.team.statisticsservice.kafka.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.statisticsservice.dto.event.StatisticsEventEnvelope;
import com.yukai.team.statisticsservice.dto.event.data.MatchResultUpdatedEventData;
import com.yukai.team.statisticsservice.entity.enums.StatisticsEventType;
import com.yukai.team.statisticsservice.service.projection.MatchSummaryProjectionService;
import com.yukai.team.statisticsservice.service.projection.TeamRankingService;
import com.yukai.team.statisticsservice.service.projection.TeamStatsProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchResultUpdatedHandler implements StatisticsEventHandler {

    private final ObjectMapper objectMapper;
    private final MatchSummaryProjectionService matchSummaryProjectionService;
    private final TeamStatsProjectionService teamStatsProjectionService;
    private final TeamRankingService teamRankingService;

    @Override
    public StatisticsEventType supports() {
        return StatisticsEventType.MATCH_RESULT_UPDATED;
    }

    @Override
    @Transactional
    public void handle(StatisticsEventEnvelope event) {
        MatchResultUpdatedEventData eventData = objectMapper.convertValue(event.getData(), MatchResultUpdatedEventData.class);
        matchSummaryProjectionService.upsertMatchResultUpdated(eventData);
        teamStatsProjectionService.applyMatchResultUpdated(eventData);
        teamRankingService.rebuildTournamentRanking(eventData.getSeasonSnapshot(), eventData.getTournamentId());
        log.info("match.result.updated processed, eventId={}, matchId={}", event.getEventId(), eventData.getMatchId());
    }
}
