package com.yukai.team.statisticsservice.service.projection;

import com.yukai.team.statisticsservice.dto.event.data.MatchAppearanceUpdatedEventData;
import com.yukai.team.statisticsservice.entity.LeaderboardProjection;
import com.yukai.team.statisticsservice.entity.PlayerStatsProjection;
import com.yukai.team.statisticsservice.entity.enums.LeaderboardBoardType;
import com.yukai.team.statisticsservice.repository.LeaderboardProjectionRepository;
import com.yukai.team.statisticsservice.repository.MatchPlayerAppearanceProjectionRepository;
import com.yukai.team.statisticsservice.repository.PlayerStatsProjectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:appearance-service;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.default_schema=statistics",
        "spring.sql.init.mode=always",
        "spring.kafka.listener.auto-startup=false"
})
@Import({AppearanceProjectionService.class, PlayerStatsProjectionService.class, LeaderboardProjectionService.class})
class AppearanceProjectionServiceTest {

    @Autowired
    private AppearanceProjectionService appearanceProjectionService;

    @Autowired
    private LeaderboardProjectionService leaderboardProjectionService;

    @Autowired
    private PlayerStatsProjectionRepository playerStatsProjectionRepository;

    @Autowired
    private MatchPlayerAppearanceProjectionRepository matchPlayerAppearanceProjectionRepository;

    @Autowired
    private LeaderboardProjectionRepository leaderboardProjectionRepository;

    @Test
    void firstAppearanceUpdatedIncrementsAppearancesAndStarts() {
        appearanceProjectionService.rebuildMatchAppearance(appearanceUpdated(
                1L,
                List.of(
                        item(1L, "Player A", true, true),
                        item(4L, "Player B", true, false)
                )
        ));

        assertThat(playerStats(1L).getAppearances()).isEqualTo(1);
        assertThat(playerStats(1L).getStarts()).isEqualTo(1);
        assertThat(playerStats(1L).getGoalInvolvements()).isZero();
        assertThat(playerStats(4L).getAppearances()).isEqualTo(1);
        assertThat(playerStats(4L).getStarts()).isZero();
        assertThat(matchPlayerAppearanceProjectionRepository.findByMatchId(1L)).hasSize(2);
    }

    @Test
    void repeatedSameAppearanceUpdatedDoesNotAccumulate() {
        MatchAppearanceUpdatedEventData eventData = appearanceUpdated(
                1L,
                List.of(
                        item(1L, "Player A", true, true),
                        item(4L, "Player B", true, false)
                )
        );

        appearanceProjectionService.rebuildMatchAppearance(eventData);
        appearanceProjectionService.rebuildMatchAppearance(eventData);

        assertThat(playerStats(1L).getAppearances()).isEqualTo(1);
        assertThat(playerStats(1L).getStarts()).isEqualTo(1);
        assertThat(playerStats(4L).getAppearances()).isEqualTo(1);
        assertThat(playerStats(4L).getStarts()).isZero();
        assertThat(matchPlayerAppearanceProjectionRepository.findByMatchId(1L)).hasSize(2);
    }

    @Test
    void changedAppearanceSnapshotRollsBackOldMatchContributionBeforeRebuild() {
        appearanceProjectionService.rebuildMatchAppearance(appearanceUpdated(
                1L,
                List.of(
                        item(1L, "Player A", true, true),
                        item(4L, "Player B", true, false)
                )
        ));

        appearanceProjectionService.rebuildMatchAppearance(appearanceUpdated(
                1L,
                List.of(
                        item(1L, "Player A", false, false),
                        item(4L, "Player B", true, true),
                        item(5L, "Player C", true, false)
                )
        ));

        assertThat(playerStats(1L).getAppearances()).isZero();
        assertThat(playerStats(1L).getStarts()).isZero();
        assertThat(playerStats(4L).getAppearances()).isEqualTo(1);
        assertThat(playerStats(4L).getStarts()).isEqualTo(1);
        assertThat(playerStats(5L).getAppearances()).isEqualTo(1);
        assertThat(playerStats(5L).getStarts()).isZero();
        assertThat(matchPlayerAppearanceProjectionRepository.findByMatchId(1L))
                .extracting("playerId")
                .containsExactlyInAnyOrder(4L, 5L);
    }

    @Test
    void appearedFalseIsSkippedAndStarterFalseDoesNotIncreaseStarts() {
        appearanceProjectionService.rebuildMatchAppearance(appearanceUpdated(
                1L,
                List.of(
                        item(1L, "Player A", false, true),
                        item(4L, "Player B", true, false)
                )
        ));

        assertThat(playerStatsProjectionRepository.findByPlayerIdAndSeasonAndTournamentId(1L, "2026", 1L)).isEmpty();
        assertThat(playerStats(4L).getAppearances()).isEqualTo(1);
        assertThat(playerStats(4L).getStarts()).isZero();
    }

    @Test
    void appearanceLeaderboardSortsByAppearancesAndSkipsZeroMetrics() {
        appearanceProjectionService.rebuildMatchAppearance(appearanceUpdated(
                1L,
                List.of(
                        item(1L, "Player A", true, true),
                        item(4L, "Player B", true, false)
                )
        ));
        appearanceProjectionService.rebuildMatchAppearance(appearanceUpdated(
                2L,
                List.of(item(4L, "Player B", true, true))
        ));
        playerStatsProjectionService().getOrCreate(9L, "Zero Metric", "2026", 1L, "Spring Cup");

        leaderboardProjectionService.rebuildLeaderboards("2026", 1L);

        List<LeaderboardProjection> appearances = leaderboardProjectionRepository
                .findByBoardTypeAndSeasonAndTournamentIdOrderByRankNoAsc(
                        LeaderboardBoardType.APPEARANCE,
                        "2026",
                        1L
                );

        assertThat(appearances).extracting(LeaderboardProjection::getEntityId).containsExactly(4L, 1L);
        assertThat(appearances).extracting(LeaderboardProjection::getMetricValue).containsExactly(2, 1);
        assertThat(appearances).extracting(LeaderboardProjection::getEntityId).doesNotContain(9L);
    }

    @Autowired
    private PlayerStatsProjectionService playerStatsProjectionService;

    private PlayerStatsProjectionService playerStatsProjectionService() {
        return playerStatsProjectionService;
    }

    private PlayerStatsProjection playerStats(Long playerId) {
        return playerStatsProjectionRepository
                .findByPlayerIdAndSeasonAndTournamentId(playerId, "2026", 1L)
                .orElseThrow();
    }

    private MatchAppearanceUpdatedEventData appearanceUpdated(
            Long matchId,
            List<MatchAppearanceUpdatedEventData.AppearanceItem> appearances
    ) {
        return MatchAppearanceUpdatedEventData.builder()
                .matchId(matchId)
                .tournamentId(1L)
                .tournamentNameSnapshot("Spring Cup")
                .seasonSnapshot("2026")
                .appearances(appearances)
                .build();
    }

    private MatchAppearanceUpdatedEventData.AppearanceItem item(
            Long playerId,
            String playerName,
            boolean appeared,
            boolean starter
    ) {
        return MatchAppearanceUpdatedEventData.AppearanceItem.builder()
                .playerId(playerId)
                .playerNameSnapshot(playerName)
                .jerseyNumberSnapshot(10)
                .positionSnapshot("FW")
                .appeared(appeared)
                .starter(starter)
                .onMinute(starter ? 0 : 60)
                .offMinute(starter ? 80 : null)
                .build();
    }
}
