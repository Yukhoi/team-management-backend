package com.yukai.team.statisticsservice.kafka.listener;

import com.yukai.team.statisticsservice.entity.MatchSummaryProjection;
import com.yukai.team.statisticsservice.entity.TeamStatsProjection;
import com.yukai.team.statisticsservice.repository.EventConsumeRecordRepository;
import com.yukai.team.statisticsservice.repository.MatchSummaryProjectionRepository;
import com.yukai.team.statisticsservice.repository.TeamStatsProjectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:team-stats-listener;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.default_schema=statistics",
        "spring.sql.init.mode=always",
        "spring.kafka.listener.auto-startup=false"
})
class TeamStatsProjectionListenerIntegrationTest {

    @Autowired
    private MatchStatisticsKafkaListener listener;

    @Autowired
    private MatchSummaryProjectionRepository matchSummaryProjectionRepository;

    @Autowired
    private TeamStatsProjectionRepository teamStatsProjectionRepository;

    @Autowired
    private EventConsumeRecordRepository eventConsumeRecordRepository;

    @Test
    void kafkaResultUpdatedMessageUpdatesSummaryTeamStatsAndConsumeRecord() {
        UUID eventId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        TestAcknowledgment acknowledgment = new TestAcknowledgment();

        listener.onMessage(matchResultUpdatedJson(eventId), acknowledgment);

        MatchSummaryProjection summary = matchSummaryProjectionRepository.findByMatchId(1L).orElseThrow();
        TeamStatsProjection stats = teamStatsProjectionRepository
                .findByTeamIdAndSeasonAndTournamentId(1L, "2026", 1L)
                .orElseThrow();

        assertThat(summary.getOurScore()).isEqualTo(2);
        assertThat(summary.getOpponentScore()).isEqualTo(1);
        assertThat(summary.getFinished()).isTrue();
        assertThat(stats.getPlayed()).isEqualTo(1);
        assertThat(stats.getWin()).isEqualTo(1);
        assertThat(stats.getGoalsFor()).isEqualTo(2);
        assertThat(stats.getGoalsAgainst()).isEqualTo(1);
        assertThat(stats.getGoalDiff()).isEqualTo(1);
        assertThat(stats.getPoints()).isEqualTo(3);
        assertThat(stats.getRankNo()).isEqualTo(1);
        assertThat(eventConsumeRecordRepository.existsByEventIdAndConsumerName(eventId, "STATISTICS_SERVICE")).isTrue();
        assertThat(acknowledgment.acknowledged).isTrue();
    }

    private String matchResultUpdatedJson(UUID eventId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "match.result.updated",
                  "aggregateType": "match",
                  "aggregateId": 1,
                  "occurredAt": "2026-05-20T02:49:39.596261+02:00",
                  "data": {
                    "matchId": 1,
                    "tournamentId": 1,
                    "tournamentNameSnapshot": "Spring Cup",
                    "seasonSnapshot": "2026",
                    "ourTeamId": 1,
                    "ourTeamNameSnapshot": "Our Team",
                    "opponentTeamId": 2,
                    "opponentTeamNameSnapshot": "Opponent A",
                    "matchTime": "2026-05-20T20:00:00+02:00",
                    "homeAway": "HOME",
                    "oldOurScore": 0,
                    "oldOpponentScore": 0,
                    "newOurScore": 2,
                    "newOpponentScore": 1,
                    "matchStatus": "FINISHED",
                    "finished": true
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
