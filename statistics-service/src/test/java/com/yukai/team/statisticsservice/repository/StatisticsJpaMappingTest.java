package com.yukai.team.statisticsservice.repository;

import com.yukai.team.statisticsservice.entity.PlayerStatsProjection;
import com.yukai.team.statisticsservice.entity.TeamStatsProjection;
import com.yukai.team.statisticsservice.entity.enums.LeaderboardBoardType;
import com.yukai.team.statisticsservice.service.consumer.EventConsumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:statistics;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.default_schema=statistics",
        "spring.sql.init.mode=always",
        "spring.kafka.listener.auto-startup=false"
})
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=localhost:9092")
@Import(EventConsumeService.class)
class StatisticsJpaMappingTest {

    @Autowired
    private PlayerStatsProjectionRepository playerStatsProjectionRepository;

    @Autowired
    private TeamStatsProjectionRepository teamStatsProjectionRepository;

    @Autowired
    private LeaderboardProjectionRepository leaderboardProjectionRepository;

    @Autowired
    private MatchSummaryProjectionRepository matchSummaryProjectionRepository;

    @Autowired
    private MatchPlayerAppearanceProjectionRepository matchPlayerAppearanceProjectionRepository;

    @Autowired
    private EventConsumeRecordRepository eventConsumeRecordRepository;

    @Autowired
    private EventConsumeService eventConsumeService;

    @Test
    void repositoriesLoadAndDerivedQueriesAreValid() {
        assertThat(playerStatsProjectionRepository.findBySeasonAndTournamentId("2026", 1L)).isEmpty();
        assertThat(teamStatsProjectionRepository.findByTeamIdAndSeasonAndTournamentId(1L, "2026", 1L)).isEmpty();
        assertThat(leaderboardProjectionRepository
                .findByBoardTypeAndSeasonAndTournamentIdOrderByRankNoAsc(
                        LeaderboardBoardType.SCORER,
                        "2026",
                        1L
                )).isEmpty();
        assertThat(matchSummaryProjectionRepository.findByMatchId(1L)).isEmpty();
        assertThat(matchPlayerAppearanceProjectionRepository.findByMatchId(1L)).isEmpty();
        assertThat(eventConsumeRecordRepository.existsByEventIdAndConsumerName(
                UUID.randomUUID(),
                "statistics-service"
        )).isFalse();
    }

    @Test
    void playerStatsUniqueConstraintIsEnforced() {
        playerStatsProjectionRepository.saveAndFlush(playerStats(1L, "2026", 1L));

        assertThatThrownBy(() -> playerStatsProjectionRepository.saveAndFlush(playerStats(1L, "2026", 1L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void helperMethodsRecalculateDerivedFields() {
        PlayerStatsProjection playerStats = playerStats(1L, "2026", 1L);
        playerStats.increaseGoals(2);
        playerStats.increaseAssists(1);

        TeamStatsProjection teamStats = TeamStatsProjection.builder()
                .teamId(1L)
                .teamNameSnapshot("Our Team")
                .season("2026")
                .tournamentId(1L)
                .tournamentNameSnapshot("Sunday League")
                .goalsFor(5)
                .goalsAgainst(2)
                .build();
        teamStats.recalculateGoalDiff();

        assertThat(playerStats.getGoalInvolvements()).isEqualTo(3);
        assertThat(teamStats.getGoalDiff()).isEqualTo(3);
    }

    @Test
    void eventConsumeServiceMarksConsumedAndHandlesDuplicateSafely() {
        UUID eventId = UUID.randomUUID();

        eventConsumeService.markConsumed(eventId, EventConsumeService.CONSUMER_NAME);
        eventConsumeService.markConsumed(eventId, EventConsumeService.CONSUMER_NAME);

        assertThat(eventConsumeService.alreadyConsumed(eventId, EventConsumeService.CONSUMER_NAME)).isTrue();
        assertThat(eventConsumeRecordRepository.count()).isEqualTo(1);
    }

    private PlayerStatsProjection playerStats(Long playerId, String season, Long tournamentId) {
        return PlayerStatsProjection.builder()
                .playerId(playerId)
                .playerNameSnapshot("Player " + playerId)
                .season(season)
                .tournamentId(tournamentId)
                .tournamentNameSnapshot("Sunday League")
                .build();
    }
}
