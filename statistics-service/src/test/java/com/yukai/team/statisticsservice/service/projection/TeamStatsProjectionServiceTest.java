package com.yukai.team.statisticsservice.service.projection;

import com.yukai.team.statisticsservice.dto.event.data.MatchResultUpdatedEventData;
import com.yukai.team.statisticsservice.entity.TeamStatsProjection;
import com.yukai.team.statisticsservice.repository.TeamStatsProjectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:team-stats-service;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.default_schema=statistics",
        "spring.sql.init.mode=always",
        "spring.kafka.listener.auto-startup=false"
})
@Import({TeamStatsProjectionService.class, TeamRankingService.class})
class TeamStatsProjectionServiceTest {

    @Autowired
    private TeamStatsProjectionService teamStatsProjectionService;

    @Autowired
    private TeamRankingService teamRankingService;

    @Autowired
    private TeamStatsProjectionRepository teamStatsProjectionRepository;

    @Test
    void firstFinishedResultAppliesWinStats() {
        teamStatsProjectionService.applyMatchResultUpdated(resultUpdated(1L, 0, 0, 2, 1, true));

        TeamStatsProjection stats = teamStats(1L);
        assertThat(stats.getPlayed()).isEqualTo(1);
        assertThat(stats.getWin()).isEqualTo(1);
        assertThat(stats.getDraw()).isZero();
        assertThat(stats.getLose()).isZero();
        assertThat(stats.getGoalsFor()).isEqualTo(2);
        assertThat(stats.getGoalsAgainst()).isEqualTo(1);
        assertThat(stats.getGoalDiff()).isEqualTo(1);
        assertThat(stats.getPoints()).isEqualTo(3);
    }

    @Test
    void scoreChangeRollsBackOldScoreBeforeApplyingNewScore() {
        teamStatsProjectionService.applyMatchResultUpdated(resultUpdated(1L, 0, 0, 2, 1, true));
        teamStatsProjectionService.applyMatchResultUpdated(resultUpdated(1L, 2, 1, 3, 1, true));

        TeamStatsProjection stats = teamStats(1L);
        assertThat(stats.getPlayed()).isEqualTo(1);
        assertThat(stats.getWin()).isEqualTo(1);
        assertThat(stats.getGoalsFor()).isEqualTo(3);
        assertThat(stats.getGoalsAgainst()).isEqualTo(1);
        assertThat(stats.getGoalDiff()).isEqualTo(2);
        assertThat(stats.getPoints()).isEqualTo(3);
    }

    @Test
    void drawResultAppliesOnePoint() {
        teamStatsProjectionService.applyMatchResultUpdated(resultUpdated(1L, 0, 0, 1, 1, true));

        TeamStatsProjection stats = teamStats(1L);
        assertThat(stats.getPlayed()).isEqualTo(1);
        assertThat(stats.getDraw()).isEqualTo(1);
        assertThat(stats.getWin()).isZero();
        assertThat(stats.getLose()).isZero();
        assertThat(stats.getPoints()).isEqualTo(1);
    }

    @Test
    void lossResultAppliesNoPoints() {
        teamStatsProjectionService.applyMatchResultUpdated(resultUpdated(1L, 0, 0, 1, 2, true));

        TeamStatsProjection stats = teamStats(1L);
        assertThat(stats.getPlayed()).isEqualTo(1);
        assertThat(stats.getLose()).isEqualTo(1);
        assertThat(stats.getWin()).isZero();
        assertThat(stats.getDraw()).isZero();
        assertThat(stats.getPoints()).isZero();
    }

    @Test
    void unfinishedResultDoesNotCountStats() {
        teamStatsProjectionService.applyMatchResultUpdated(resultUpdated(1L, 0, 0, 2, 1, false));

        TeamStatsProjection stats = teamStats(1L);
        assertThat(stats.getPlayed()).isZero();
        assertThat(stats.getWin()).isZero();
        assertThat(stats.getDraw()).isZero();
        assertThat(stats.getLose()).isZero();
        assertThat(stats.getGoalsFor()).isZero();
        assertThat(stats.getGoalsAgainst()).isZero();
        assertThat(stats.getGoalDiff()).isZero();
        assertThat(stats.getPoints()).isZero();
    }

    @Test
    void rankingRebuildSortsByPointsGoalDiffGoalsForAndTeamId() {
        teamStatsProjectionRepository.save(teamStats(1L, 6, 2, 5));
        teamStatsProjectionRepository.save(teamStats(2L, 7, 0, 4));
        teamStatsProjectionRepository.save(teamStats(3L, 6, 4, 5));
        teamStatsProjectionRepository.save(teamStats(4L, 6, 4, 6));
        teamStatsProjectionRepository.save(teamStats(5L, 6, 4, 6));

        teamRankingService.rebuildTournamentRanking("2026", 1L);

        assertThat(teamStats(2L).getRankNo()).isEqualTo(1);
        assertThat(teamStats(4L).getRankNo()).isEqualTo(2);
        assertThat(teamStats(5L).getRankNo()).isEqualTo(3);
        assertThat(teamStats(3L).getRankNo()).isEqualTo(4);
        assertThat(teamStats(1L).getRankNo()).isEqualTo(5);
    }

    private TeamStatsProjection teamStats(Long teamId) {
        return teamStatsProjectionRepository
                .findByTeamIdAndSeasonAndTournamentId(teamId, "2026", 1L)
                .orElseThrow();
    }

    private TeamStatsProjection teamStats(Long teamId, int points, int goalDiff, int goalsFor) {
        return TeamStatsProjection.builder()
                .teamId(teamId)
                .teamNameSnapshot("Team " + teamId)
                .season("2026")
                .tournamentId(1L)
                .tournamentNameSnapshot("Spring Cup")
                .played(1)
                .win(0)
                .draw(0)
                .lose(0)
                .goalsFor(goalsFor)
                .goalsAgainst(goalsFor - goalDiff)
                .goalDiff(goalDiff)
                .points(points)
                .build();
    }

    private MatchResultUpdatedEventData resultUpdated(
            Long teamId,
            int oldOurScore,
            int oldOpponentScore,
            int newOurScore,
            int newOpponentScore,
            boolean finished
    ) {
        return MatchResultUpdatedEventData.builder()
                .matchId(1L)
                .tournamentId(1L)
                .tournamentNameSnapshot("Spring Cup")
                .seasonSnapshot("2026")
                .ourTeamId(teamId)
                .ourTeamNameSnapshot("Our Team")
                .opponentTeamId(2L)
                .opponentTeamNameSnapshot("Opponent A")
                .matchTime(OffsetDateTime.parse("2026-05-20T20:00:00+02:00"))
                .homeAway("HOME")
                .oldOurScore(oldOurScore)
                .oldOpponentScore(oldOpponentScore)
                .newOurScore(newOurScore)
                .newOpponentScore(newOpponentScore)
                .matchStatus(finished ? "FINISHED" : "SCHEDULED")
                .finished(finished)
                .build();
    }
}
