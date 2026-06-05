package com.yukai.team.statisticsservice.kafka.listener;

import com.yukai.team.statisticsservice.entity.MatchSummaryProjection;
import com.yukai.team.statisticsservice.repository.EventConsumeRecordRepository;
import com.yukai.team.statisticsservice.repository.MatchSummaryProjectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:match-summary-listener;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.default_schema=statistics",
        "spring.sql.init.mode=always",
        "spring.kafka.listener.auto-startup=false"
})
class MatchSummaryProjectionListenerIntegrationTest {

    @Autowired
    private MatchStatisticsKafkaListener listener;

    @Autowired
    private MatchSummaryProjectionRepository matchSummaryProjectionRepository;

    @Autowired
    private EventConsumeRecordRepository eventConsumeRecordRepository;

    @Test
    void kafkaMessageCreatesMatchSummaryAndConsumeRecord() {
        TestAcknowledgment acknowledgment = new TestAcknowledgment();
        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        listener.onMessage(matchCreatedJson(eventId), acknowledgment);

        MatchSummaryProjection projection = matchSummaryProjectionRepository.findByMatchId(1L).orElseThrow();
        assertThat(projection.getTournamentNameSnapshot()).isEqualTo("Spring Cup");
        assertThat(projection.getSeasonSnapshot()).isEqualTo("2026");
        assertThat(projection.getOurScore()).isZero();
        assertThat(eventConsumeRecordRepository.existsByEventIdAndConsumerName(eventId, "STATISTICS_SERVICE")).isTrue();
        assertThat(acknowledgment.acknowledged).isTrue();
    }

    private String matchCreatedJson(UUID eventId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "match.created",
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
                    "ourScore": 0,
                    "opponentScore": 0,
                    "matchStatus": "SCHEDULED",
                    "finished": false
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
