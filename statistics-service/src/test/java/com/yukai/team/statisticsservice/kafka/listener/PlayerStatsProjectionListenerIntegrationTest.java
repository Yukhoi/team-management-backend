package com.yukai.team.statisticsservice.kafka.listener;

import com.yukai.team.statisticsservice.entity.PlayerStatsProjection;
import com.yukai.team.statisticsservice.entity.enums.LeaderboardBoardType;
import com.yukai.team.statisticsservice.repository.EventConsumeRecordRepository;
import com.yukai.team.statisticsservice.repository.LeaderboardProjectionRepository;
import com.yukai.team.statisticsservice.repository.PlayerStatsProjectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:player-stats-listener;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.default_schema=statistics",
        "spring.sql.init.mode=always",
        "spring.kafka.listener.auto-startup=false"
})
class PlayerStatsProjectionListenerIntegrationTest {

    @Autowired
    private MatchStatisticsKafkaListener listener;

    @Autowired
    private PlayerStatsProjectionRepository playerStatsProjectionRepository;

    @Autowired
    private LeaderboardProjectionRepository leaderboardProjectionRepository;

    @Autowired
    private EventConsumeRecordRepository eventConsumeRecordRepository;

    @BeforeEach
    void setUp() {
        leaderboardProjectionRepository.deleteAll();
        playerStatsProjectionRepository.deleteAll();
        eventConsumeRecordRepository.deleteAll();
    }

    @Test
    void kafkaGoalCreatedMessageUpdatesPlayerStatsAndConsumeRecord() {
        UUID eventId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        TestAcknowledgment acknowledgment = new TestAcknowledgment();

        listener.onMessage(goalCreatedJson(eventId), acknowledgment);

        PlayerStatsProjection stats = playerStatsProjectionRepository
                .findByPlayerIdAndSeasonAndTournamentId(1L, "2026", 1L)
                .orElseThrow();

        assertThat(stats.getGoals()).isEqualTo(1);
        assertThat(stats.getAssists()).isZero();
        assertThat(stats.getGoalInvolvements()).isEqualTo(1);
        assertThat(eventConsumeRecordRepository.existsByEventIdAndConsumerName(eventId, "STATISTICS_SERVICE")).isTrue();
        assertThat(acknowledgment.acknowledged).isTrue();
    }

    @Test
    void kafkaAssistUpsertedMessageUpdatesPlayerStatsLeaderboardsAndConsumeRecord() {
        UUID eventId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        TestAcknowledgment acknowledgment = new TestAcknowledgment();

        listener.onMessage(assistUpsertedJson(eventId, "match.assist.upserted"), acknowledgment);

        assertAssistProjectionUpdated(eventId, acknowledgment);

        TestAcknowledgment duplicateAcknowledgment = new TestAcknowledgment();
        listener.onMessage(assistUpsertedJson(eventId, "match.assist.upserted"), duplicateAcknowledgment);

        assertAssistProjectionUpdated(eventId, duplicateAcknowledgment);
    }

    @Test
    void historicalAssistCreatedAndUpdatedMessagesUseUpsertHandler() {
        UUID createdEventId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        listener.onMessage(assistUpsertedJson(createdEventId, "match.assist.created"), new TestAcknowledgment());

        UUID updatedEventId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        TestAcknowledgment acknowledgment = new TestAcknowledgment();
        listener.onMessage(assistUpdatedJson(updatedEventId, "match.assist.updated"), acknowledgment);

        PlayerStatsProjection oldAssister = playerStatsProjectionRepository
                .findByPlayerIdAndSeasonAndTournamentId(4L, "2026", 1L)
                .orElseThrow();
        PlayerStatsProjection newAssister = playerStatsProjectionRepository
                .findByPlayerIdAndSeasonAndTournamentId(5L, "2026", 1L)
                .orElseThrow();

        assertThat(oldAssister.getAssists()).isZero();
        assertThat(oldAssister.getGoalInvolvements()).isZero();
        assertThat(newAssister.getAssists()).isEqualTo(1);
        assertThat(newAssister.getGoalInvolvements()).isEqualTo(1);
        assertThat(eventConsumeRecordRepository.existsByEventIdAndConsumerName(createdEventId, "STATISTICS_SERVICE")).isTrue();
        assertThat(eventConsumeRecordRepository.existsByEventIdAndConsumerName(updatedEventId, "STATISTICS_SERVICE")).isTrue();
        assertThat(acknowledgment.acknowledged).isTrue();
    }

    @Test
    void unknownEventDoesNotUpdateProjectionWriteConsumeRecordOrAcknowledge() {
        UUID eventId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        TestAcknowledgment acknowledgment = new TestAcknowledgment();

        assertThatThrownBy(() -> listener.onMessage(unknownEventJson(eventId), acknowledgment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported eventType");

        assertThat(playerStatsProjectionRepository.findAll()).isEmpty();
        assertThat(leaderboardProjectionRepository.findAll()).isEmpty();
        assertThat(eventConsumeRecordRepository.existsByEventIdAndConsumerName(eventId, "STATISTICS_SERVICE")).isFalse();
        assertThat(acknowledgment.acknowledged).isFalse();
    }

    @Test
    void kafkaGoalDeletedWithLinkedAssistRollsBackGoalAssistAndLeaderboards() {
        listener.onMessage(goalCreatedJson(UUID.fromString("33333333-3333-3333-3333-333333333333")), new TestAcknowledgment());
        listener.onMessage(assistUpsertedJson(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "match.assist.upserted"
        ), new TestAcknowledgment());

        UUID deleteEventId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        TestAcknowledgment acknowledgment = new TestAcknowledgment();
        listener.onMessage(goalDeletedWithLinkedAssistJson(deleteEventId), acknowledgment);

        PlayerStatsProjection scorer = playerStatsProjectionRepository
                .findByPlayerIdAndSeasonAndTournamentId(1L, "2026", 1L)
                .orElseThrow();
        PlayerStatsProjection assister = playerStatsProjectionRepository
                .findByPlayerIdAndSeasonAndTournamentId(4L, "2026", 1L)
                .orElseThrow();

        assertThat(scorer.getGoals()).isZero();
        assertThat(scorer.getGoalInvolvements()).isZero();
        assertThat(assister.getAssists()).isZero();
        assertThat(assister.getGoalInvolvements()).isZero();
        assertThat(leaderboardProjectionRepository.findByBoardTypeAndSeasonAndTournamentIdOrderByRankNoAsc(
                LeaderboardBoardType.SCORER,
                "2026",
                1L
        )).isEmpty();
        assertThat(leaderboardProjectionRepository.findByBoardTypeAndSeasonAndTournamentIdOrderByRankNoAsc(
                LeaderboardBoardType.ASSIST,
                "2026",
                1L
        )).isEmpty();
        assertThat(eventConsumeRecordRepository.existsByEventIdAndConsumerName(deleteEventId, "STATISTICS_SERVICE")).isTrue();
        assertThat(acknowledgment.acknowledged).isTrue();
    }

    private String goalCreatedJson(UUID eventId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "match.goal.created",
                  "aggregateType": "match",
                  "aggregateId": 1,
                  "occurredAt": "2026-05-20T17:36:30.3362+02:00",
                  "data": {
                    "goalId": 1,
                    "matchId": 1,
                    "tournamentId": 1,
                    "tournamentNameSnapshot": "Spring Cup",
                    "seasonSnapshot": "2026",
                    "playerId": 1,
                    "playerNameSnapshot": "Player A",
                    "jerseyNumberSnapshot": 10,
                    "goalMinute": 36,
                    "goalType": "NORMAL"
                  }
                }
                """.formatted(eventId);
    }

    private void assertAssistProjectionUpdated(UUID eventId, TestAcknowledgment acknowledgment) {
        PlayerStatsProjection stats = playerStatsProjectionRepository
                .findByPlayerIdAndSeasonAndTournamentId(4L, "2026", 1L)
                .orElseThrow();

        assertThat(stats.getAssists()).isEqualTo(1);
        assertThat(stats.getGoalInvolvements()).isEqualTo(1);
        assertThat(leaderboardProjectionRepository.findByBoardTypeAndSeasonAndTournamentIdOrderByRankNoAsc(
                LeaderboardBoardType.ASSIST,
                "2026",
                1L
        )).singleElement().satisfies(entry -> {
            assertThat(entry.getEntityId()).isEqualTo(4L);
            assertThat(entry.getMetricValue()).isEqualTo(1);
        });
        assertThat(eventConsumeRecordRepository.existsByEventIdAndConsumerName(eventId, "STATISTICS_SERVICE")).isTrue();
        assertThat(acknowledgment.acknowledged).isTrue();
    }

    private String assistUpsertedJson(UUID eventId, String eventType) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "aggregateType": "match",
                  "aggregateId": 1,
                  "occurredAt": "2026-05-20T17:58:25.13628+02:00",
                  "data": {
                    "assistId": 1,
                    "goalId": 1,
                    "matchId": 1,
                    "tournamentId": 1,
                    "tournamentNameSnapshot": "Spring Cup",
                    "seasonSnapshot": "2026",
                    "oldAssist": null,
                    "newAssist": {
                      "playerId": 4,
                      "playerNameSnapshot": "Player B",
                      "jerseyNumberSnapshot": 8,
                      "assistMinute": 36
                    }
                  }
                }
                """.formatted(eventId, eventType);
    }

    private String assistUpdatedJson(UUID eventId, String eventType) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "aggregateType": "match",
                  "aggregateId": 1,
                  "occurredAt": "2026-05-20T17:59:25.13628+02:00",
                  "data": {
                    "assistId": 1,
                    "goalId": 1,
                    "matchId": 1,
                    "tournamentId": 1,
                    "tournamentNameSnapshot": "Spring Cup",
                    "seasonSnapshot": "2026",
                    "oldAssist": {
                      "playerId": 4,
                      "playerNameSnapshot": "Player B",
                      "jerseyNumberSnapshot": 8,
                      "assistMinute": 36
                    },
                    "newAssist": {
                      "playerId": 5,
                      "playerNameSnapshot": "Player C",
                      "jerseyNumberSnapshot": 9,
                      "assistMinute": 37
                    }
                  }
                }
                """.formatted(eventId, eventType);
    }

    private String unknownEventJson(UUID eventId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "unknown.event",
                  "aggregateType": "match",
                  "aggregateId": 1,
                  "occurredAt": "2026-05-20T18:10:30.3362+02:00",
                  "data": {}
                }
                """.formatted(eventId);
    }

    private String goalDeletedWithLinkedAssistJson(UUID eventId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "match.goal.deleted",
                  "aggregateType": "match",
                  "aggregateId": 1,
                  "occurredAt": "2026-05-20T18:05:30.3362+02:00",
                  "data": {
                    "goalId": 1,
                    "matchId": 1,
                    "tournamentId": 1,
                    "tournamentNameSnapshot": "Spring Cup",
                    "seasonSnapshot": "2026",
                    "playerId": 1,
                    "playerNameSnapshot": "Player A",
                    "jerseyNumberSnapshot": 10,
                    "goalMinute": 36,
                    "goalType": "NORMAL",
                    "linkedAssist": {
                      "assistId": 1,
                      "playerId": 4,
                      "playerNameSnapshot": "Player B",
                      "jerseyNumberSnapshot": 8,
                      "assistMinute": 36
                    }
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
