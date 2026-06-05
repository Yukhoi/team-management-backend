package com.yukai.team.statisticsservice.service.projection;

import com.yukai.team.statisticsservice.dto.event.data.MatchAssistDeletedEventData;
import com.yukai.team.statisticsservice.dto.event.data.MatchAssistUpsertedEventData;
import com.yukai.team.statisticsservice.dto.event.data.MatchGoalCreatedEventData;
import com.yukai.team.statisticsservice.dto.event.data.MatchGoalDeletedEventData;
import com.yukai.team.statisticsservice.dto.event.data.MatchGoalUpdatedEventData;
import com.yukai.team.statisticsservice.entity.LeaderboardProjection;
import com.yukai.team.statisticsservice.entity.PlayerStatsProjection;
import com.yukai.team.statisticsservice.entity.enums.LeaderboardBoardType;
import com.yukai.team.statisticsservice.repository.LeaderboardProjectionRepository;
import com.yukai.team.statisticsservice.repository.PlayerStatsProjectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:player-stats-service;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.default_schema=statistics",
        "spring.sql.init.mode=always",
        "spring.kafka.listener.auto-startup=false"
})
@Import({PlayerStatsProjectionService.class, LeaderboardProjectionService.class})
class PlayerStatsProjectionServiceTest {

    @Autowired
    private PlayerStatsProjectionService playerStatsProjectionService;

    @Autowired
    private LeaderboardProjectionService leaderboardProjectionService;

    @Autowired
    private PlayerStatsProjectionRepository playerStatsProjectionRepository;

    @Autowired
    private LeaderboardProjectionRepository leaderboardProjectionRepository;

    @Test
    void normalGoalCreatedIncrementsGoalsAndGoalInvolvements() {
        playerStatsProjectionService.applyGoalCreated(goalCreated(1L, "Player A", "NORMAL"));

        PlayerStatsProjection stats = playerStats(1L);
        assertThat(stats.getGoals()).isEqualTo(1);
        assertThat(stats.getGoalInvolvements()).isEqualTo(1);
    }

    @Test
    void ownGoalCreatedIsIgnored() {
        playerStatsProjectionService.applyGoalCreated(goalCreated(null, null, "OWN_GOAL"));

        assertThat(playerStatsProjectionRepository.findBySeasonAndTournamentId("2026", 1L)).isEmpty();
    }

    @Test
    void goalDeletedRollsBackGoalWithoutNegativeValue() {
        playerStatsProjectionService.applyGoalCreated(goalCreated(1L, "Player A", "NORMAL"));
        playerStatsProjectionService.applyGoalDeleted(goalDeleted(1L, "Player A", "NORMAL"));
        playerStatsProjectionService.applyGoalDeleted(goalDeleted(1L, "Player A", "NORMAL"));

        PlayerStatsProjection stats = playerStats(1L);
        assertThat(stats.getGoals()).isZero();
        assertThat(stats.getGoalInvolvements()).isZero();
    }

    @Test
    void goalDeletedWithLinkedAssistRollsBackGoalAndAssist() {
        playerStatsProjectionService.applyGoalCreated(goalCreated(1L, "Player A", "NORMAL"));
        playerStatsProjectionService.applyAssistUpserted(assistUpserted(null, newAssistSnapshot(4L, "Player B")));

        playerStatsProjectionService.applyGoalDeleted(goalDeletedWithAssist(
                1L,
                "Player A",
                "NORMAL",
                linkedAssist(4L, "Player B")
        ));

        assertThat(playerStats(1L).getGoals()).isZero();
        assertThat(playerStats(1L).getGoalInvolvements()).isZero();
        assertThat(playerStats(4L).getAssists()).isZero();
        assertThat(playerStats(4L).getGoalInvolvements()).isZero();
    }

    @Test
    void goalUpdatedMovesGoalBetweenPlayers() {
        playerStatsProjectionService.applyGoalCreated(goalCreated(1L, "Player A", "NORMAL"));
        playerStatsProjectionService.applyGoalUpdated(goalUpdated(
                goalSnapshot(1L, "Player A", "NORMAL"),
                newGoalSnapshot(2L, "Player B", "PENALTY")
        ));

        assertThat(playerStats(1L).getGoals()).isZero();
        assertThat(playerStats(2L).getGoals()).isEqualTo(1);
        assertThat(playerStats(2L).getGoalInvolvements()).isEqualTo(1);
    }

    @Test
    void goalUpdatedFromNormalToOwnGoalRollsBackPlayerGoal() {
        playerStatsProjectionService.applyGoalCreated(goalCreated(1L, "Player A", "NORMAL"));
        playerStatsProjectionService.applyGoalUpdated(goalUpdated(
                goalSnapshot(1L, "Player A", "NORMAL"),
                newGoalSnapshot(null, null, "OWN_GOAL")
        ));

        PlayerStatsProjection stats = playerStats(1L);
        assertThat(stats.getGoals()).isZero();
        assertThat(stats.getGoalInvolvements()).isZero();
    }

    @Test
    void assistUpsertedCreatesAndMovesAssist() {
        playerStatsProjectionService.applyAssistUpserted(assistUpserted(
                null,
                newAssistSnapshot(4L, "Player B")
        ));
        playerStatsProjectionService.applyAssistUpserted(assistUpserted(
                assistSnapshot(4L, "Player B"),
                newAssistSnapshot(5L, "Player C")
        ));

        assertThat(playerStats(4L).getAssists()).isZero();
        assertThat(playerStats(5L).getAssists()).isEqualTo(1);
        assertThat(playerStats(5L).getGoalInvolvements()).isEqualTo(1);
    }

    @Test
    void assistDeletedRollsBackAssistWithoutNegativeValue() {
        playerStatsProjectionService.applyAssistUpserted(assistUpserted(null, newAssistSnapshot(4L, "Player B")));
        playerStatsProjectionService.applyAssistDeleted(assistDeleted(4L, "Player B"));
        playerStatsProjectionService.applyAssistDeleted(assistDeleted(4L, "Player B"));

        PlayerStatsProjection stats = playerStats(4L);
        assertThat(stats.getAssists()).isZero();
        assertThat(stats.getGoalInvolvements()).isZero();
    }

    @Test
    void leaderboardRebuildSortsAndSkipsZeroMetrics() {
        playerStatsProjectionService.applyGoalCreated(goalCreated(1L, "Player A", "NORMAL"));
        playerStatsProjectionService.applyGoalCreated(goalCreated(2L, "Player B", "NORMAL"));
        playerStatsProjectionService.applyGoalCreated(goalCreated(2L, "Player B", "PENALTY"));
        playerStatsProjectionService.applyAssistUpserted(assistUpserted(null, newAssistSnapshot(3L, "Player C")));
        playerStatsProjectionService.getOrCreate(9L, "Zero Metric", "2026", 1L, "Spring Cup");

        leaderboardProjectionService.rebuildLeaderboards("2026", 1L);

        List<LeaderboardProjection> scorers = leaderboardProjectionRepository
                .findByBoardTypeAndSeasonAndTournamentIdOrderByRankNoAsc(
                        LeaderboardBoardType.SCORER,
                        "2026",
                        1L
                );
        List<LeaderboardProjection> assists = leaderboardProjectionRepository
                .findByBoardTypeAndSeasonAndTournamentIdOrderByRankNoAsc(
                        LeaderboardBoardType.ASSIST,
                        "2026",
                        1L
                );

        assertThat(scorers).extracting(LeaderboardProjection::getEntityId).containsExactly(2L, 1L);
        assertThat(scorers).extracting(LeaderboardProjection::getMetricValue).containsExactly(2, 1);
        assertThat(assists).extracting(LeaderboardProjection::getEntityId).containsExactly(3L);
        assertThat(scorers).extracting(LeaderboardProjection::getEntityId).doesNotContain(9L);
    }

    private PlayerStatsProjection playerStats(Long playerId) {
        return playerStatsProjectionRepository
                .findByPlayerIdAndSeasonAndTournamentId(playerId, "2026", 1L)
                .orElseThrow();
    }

    private MatchGoalCreatedEventData goalCreated(Long playerId, String playerName, String goalType) {
        return MatchGoalCreatedEventData.builder()
                .goalId(1L)
                .matchId(1L)
                .tournamentId(1L)
                .tournamentNameSnapshot("Spring Cup")
                .seasonSnapshot("2026")
                .playerId(playerId)
                .playerNameSnapshot(playerName)
                .jerseyNumberSnapshot(10)
                .goalMinute(36)
                .goalType(goalType)
                .build();
    }

    private MatchGoalDeletedEventData goalDeleted(Long playerId, String playerName, String goalType) {
        return MatchGoalDeletedEventData.builder()
                .goalId(1L)
                .matchId(1L)
                .tournamentId(1L)
                .tournamentNameSnapshot("Spring Cup")
                .seasonSnapshot("2026")
                .playerId(playerId)
                .playerNameSnapshot(playerName)
                .jerseyNumberSnapshot(10)
                .goalMinute(36)
                .goalType(goalType)
                .build();
    }

    private MatchGoalDeletedEventData goalDeletedWithAssist(
            Long playerId,
            String playerName,
            String goalType,
            MatchGoalDeletedEventData.LinkedAssist linkedAssist
    ) {
        return MatchGoalDeletedEventData.builder()
                .goalId(1L)
                .matchId(1L)
                .tournamentId(1L)
                .tournamentNameSnapshot("Spring Cup")
                .seasonSnapshot("2026")
                .playerId(playerId)
                .playerNameSnapshot(playerName)
                .jerseyNumberSnapshot(10)
                .goalMinute(36)
                .goalType(goalType)
                .linkedAssist(linkedAssist)
                .build();
    }

    private MatchGoalDeletedEventData.LinkedAssist linkedAssist(Long playerId, String playerName) {
        return MatchGoalDeletedEventData.LinkedAssist.builder()
                .assistId(1L)
                .playerId(playerId)
                .playerNameSnapshot(playerName)
                .jerseyNumberSnapshot(8)
                .assistMinute(36)
                .build();
    }

    private MatchGoalUpdatedEventData goalUpdated(
            MatchGoalUpdatedEventData.OldGoal oldGoal,
            MatchGoalUpdatedEventData.NewGoal newGoal
    ) {
        return MatchGoalUpdatedEventData.builder()
                .goalId(1L)
                .matchId(1L)
                .tournamentId(1L)
                .tournamentNameSnapshot("Spring Cup")
                .seasonSnapshot("2026")
                .oldGoal(oldGoal)
                .newGoal(newGoal)
                .build();
    }

    private MatchGoalUpdatedEventData.OldGoal goalSnapshot(Long playerId, String playerName, String goalType) {
        return MatchGoalUpdatedEventData.OldGoal.builder()
                .playerId(playerId)
                .playerNameSnapshot(playerName)
                .jerseyNumberSnapshot(10)
                .goalType(goalType)
                .build();
    }

    private MatchGoalUpdatedEventData.NewGoal newGoalSnapshot(Long playerId, String playerName, String goalType) {
        return MatchGoalUpdatedEventData.NewGoal.builder()
                .playerId(playerId)
                .playerNameSnapshot(playerName)
                .jerseyNumberSnapshot(10)
                .goalType(goalType)
                .build();
    }

    private MatchAssistUpsertedEventData assistUpserted(
            MatchAssistUpsertedEventData.OldAssist oldAssist,
            MatchAssistUpsertedEventData.NewAssist newAssist
    ) {
        return MatchAssistUpsertedEventData.builder()
                .assistId(1L)
                .goalId(1L)
                .matchId(1L)
                .tournamentId(1L)
                .tournamentNameSnapshot("Spring Cup")
                .seasonSnapshot("2026")
                .oldAssist(oldAssist)
                .newAssist(newAssist)
                .build();
    }

    private MatchAssistUpsertedEventData.OldAssist assistSnapshot(Long playerId, String playerName) {
        return MatchAssistUpsertedEventData.OldAssist.builder()
                .playerId(playerId)
                .playerNameSnapshot(playerName)
                .jerseyNumberSnapshot(8)
                .assistMinute(36)
                .build();
    }

    private MatchAssistUpsertedEventData.NewAssist newAssistSnapshot(Long playerId, String playerName) {
        return MatchAssistUpsertedEventData.NewAssist.builder()
                .playerId(playerId)
                .playerNameSnapshot(playerName)
                .jerseyNumberSnapshot(8)
                .assistMinute(36)
                .build();
    }

    private MatchAssistDeletedEventData assistDeleted(Long playerId, String playerName) {
        return MatchAssistDeletedEventData.builder()
                .assistId(1L)
                .goalId(1L)
                .matchId(1L)
                .tournamentId(1L)
                .tournamentNameSnapshot("Spring Cup")
                .seasonSnapshot("2026")
                .playerId(playerId)
                .playerNameSnapshot(playerName)
                .jerseyNumberSnapshot(8)
                .assistMinute(36)
                .build();
    }
}
