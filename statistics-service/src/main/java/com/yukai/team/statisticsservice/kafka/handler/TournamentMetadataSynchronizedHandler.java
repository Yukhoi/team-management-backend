package com.yukai.team.statisticsservice.kafka.handler;

import com.yukai.team.statisticsservice.dto.event.StatisticsEventEnvelope;
import com.yukai.team.statisticsservice.entity.enums.StatisticsEventType;
import com.yukai.team.statisticsservice.service.projection.TournamentMetadataProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TournamentMetadataSynchronizedHandler implements StatisticsEventHandler {
    private final TournamentMetadataProjectionService metadata;
    @Override
    public StatisticsEventType supports() { return StatisticsEventType.TOURNAMENT_METADATA_SYNCHRONIZED; }
    @Override
    public void handle(StatisticsEventEnvelope event) { metadata.apply(event); }
}
