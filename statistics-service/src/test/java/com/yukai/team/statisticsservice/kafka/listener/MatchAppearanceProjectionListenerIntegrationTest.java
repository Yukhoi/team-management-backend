package com.yukai.team.statisticsservice.kafka.listener;

import com.yukai.team.statisticsservice.entity.PlayerStatsProjection;
import com.yukai.team.statisticsservice.repository.EventConsumeRecordRepository;
import com.yukai.team.statisticsservice.repository.MatchPlayerAppearanceProjectionRepository;
import com.yukai.team.statisticsservice.repository.PlayerStatsProjectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:appearance-listener;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.default_schema=statistics",
        "spring.sql.init.mode=always",
        "spring.kafka.listener.auto-startup=false"
})
class MatchAppearanceProjectionListenerIntegrationTest {

    @Autowired
    private MatchStatisticsKafkaListener listener;

    @Autowired
    private PlayerStatsProjectionRepository playerStatsProjectionRepository;

    @Autowired
    private MatchPlayerAppearanceProjectionRepository matchPlayerAppearanceProjectionRepository;

    @Autowired
    private EventConsumeRecordRepository eventConsumeRecordRepository;

    @Test
    void kafkaAppearanceUpdatedMessageUpdatesPlayerStatsAndConsumeRecord() {
        UUID eventId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        TestAcknowledgment acknowledgment = new TestAcknowledgment();

        listener.onMessage(appearanceUpdatedJson(eventId), acknowledgment);

        PlayerStatsProjection playerA = playerStatsProjectionRepository
                .findByPlayerIdAndSeasonAndTournamentId(1L, "2026", 1L)
                .orElseThrow();
        PlayerStatsProjection playerB = playerStatsProjectionRepository
                .findByPlayerIdAndSeasonAndTournamentId(4L, "2026", 1L)
                .orElseThrow();

        assertThat(playerA.getAppearances()).isEqualTo(1);
        assertThat(playerA.getStarts()).isEqualTo(1);
        assertThat(playerA.getGoalInvolvements()).isZero();
        assertThat(playerB.getAppearances()).isEqualTo(1);
        assertThat(playerB.getStarts()).isZero();
        assertThat(matchPlayerAppearanceProjectionRepository.findByMatchId(1L)).hasSize(2);
        assertThat(eventConsumeRecordRepository.existsByEventIdAndConsumerName(eventId, "STATISTICS_SERVICE")).isTrue();
        assertThat(acknowledgment.acknowledged).isTrue();
    }

    private String appearanceUpdatedJson(UUID eventId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "match.appearance.updated",
                  "aggregateType": "match",
                  "aggregateId": 1,
                  "occurredAt": "2026-05-20T17:58:09.537323+02:00",
                  "data": {
                    "matchId": 1,
                    "tournamentId": 1,
                    "tournamentNameSnapshot": "Spring Cup",
                    "seasonSnapshot": "2026",
                    "appearances": [
                      {
                        "playerId": 1,
                        "playerNameSnapshot": "Player A",
                        "jerseyNumberSnapshot": 10,
                        "positionSnapshot": "FW",
                        "appeared": true,
                        "starter": true,
                        "onMinute": 0,
                        "offMinute": 80
                      },
                      {
                        "playerId": 4,
                        "playerNameSnapshot": "Player B",
                        "jerseyNumberSnapshot": 8,
                        "positionSnapshot": "MF",
                        "appeared": true,
                        "starter": false,
                        "onMinute": 60,
                        "offMinute": null
                      }
                    ]
                  }
                }
                """.formatted(eventId);
    }

    private static class TestAcknowledgment implements Acknowledgment {

        private boolean acknowledged;

        @Override
        public void acknowledge() {
            acknowledged = true;
        }
    }
}
