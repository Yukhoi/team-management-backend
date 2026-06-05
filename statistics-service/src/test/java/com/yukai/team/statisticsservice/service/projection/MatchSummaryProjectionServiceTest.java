package com.yukai.team.statisticsservice.service.projection;

import com.yukai.team.statisticsservice.dto.event.data.MatchCreatedEventData;
import com.yukai.team.statisticsservice.dto.event.data.MatchResultUpdatedEventData;
import com.yukai.team.statisticsservice.entity.MatchSummaryProjection;
import com.yukai.team.statisticsservice.entity.enums.HomeAwayType;
import com.yukai.team.statisticsservice.entity.enums.MatchStatus;
import com.yukai.team.statisticsservice.repository.MatchSummaryProjectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:match-summary-service;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.default_schema=statistics",
        "spring.sql.init.mode=always",
        "spring.kafka.listener.auto-startup=false"
})
@Import(MatchSummaryProjectionService.class)
class MatchSummaryProjectionServiceTest {

    @Autowired
    private MatchSummaryProjectionService matchSummaryProjectionService;

    @Autowired
    private MatchSummaryProjectionRepository matchSummaryProjectionRepository;

    @Test
    void matchCreatedCreatesProjection() {
        matchSummaryProjectionService.upsertMatchCreated(matchCreated(1L, "Spring Cup"));

        MatchSummaryProjection projection = matchSummaryProjectionRepository.findByMatchId(1L).orElseThrow();
        assertThat(projection.getMatchId()).isEqualTo(1L);
        assertThat(projection.getSeasonSnapshot()).isEqualTo("2026");
        assertThat(projection.getTournamentNameSnapshot()).isEqualTo("Spring Cup");
        assertThat(projection.getHomeAway()).isEqualTo(HomeAwayType.HOME);
        assertThat(projection.getOurScore()).isZero();
        assertThat(projection.getOpponentScore()).isZero();
        assertThat(projection.getMatchStatus()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(projection.getFinished()).isFalse();
    }

    @Test
    void repeatedMatchCreatedUpdatesExistingProjection() {
        matchSummaryProjectionService.upsertMatchCreated(matchCreated(1L, "Spring Cup"));
        matchSummaryProjectionService.upsertMatchCreated(matchCreated(1L, "Updated Cup"));

        MatchSummaryProjection projection = matchSummaryProjectionRepository.findByMatchId(1L).orElseThrow();
        assertThat(matchSummaryProjectionRepository.count()).isEqualTo(1);
        assertThat(projection.getTournamentNameSnapshot()).isEqualTo("Updated Cup");
    }

    @Test
    void matchResultUpdatedUpdatesScoreAndStatus() {
        matchSummaryProjectionService.upsertMatchCreated(matchCreated(1L, "Spring Cup"));
        matchSummaryProjectionService.upsertMatchResultUpdated(matchResultUpdated(1L));

        MatchSummaryProjection projection = matchSummaryProjectionRepository.findByMatchId(1L).orElseThrow();
        assertThat(projection.getOurScore()).isEqualTo(2);
        assertThat(projection.getOpponentScore()).isEqualTo(1);
        assertThat(projection.getMatchStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(projection.getFinished()).isTrue();
    }

    @Test
    void resultUpdatedBeforeCreatedKeepsFinalResult() {
        matchSummaryProjectionService.upsertMatchResultUpdated(matchResultUpdated(1L));
        matchSummaryProjectionService.upsertMatchCreated(matchCreated(1L, "Spring Cup"));

        MatchSummaryProjection projection = matchSummaryProjectionRepository.findByMatchId(1L).orElseThrow();
        assertThat(matchSummaryProjectionRepository.count()).isEqualTo(1);
        assertThat(projection.getTournamentNameSnapshot()).isEqualTo("Spring Cup");
        assertThat(projection.getOurScore()).isEqualTo(2);
        assertThat(projection.getOpponentScore()).isEqualTo(1);
        assertThat(projection.getMatchStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(projection.getFinished()).isTrue();
    }

    private MatchCreatedEventData matchCreated(Long matchId, String tournamentName) {
        return MatchCreatedEventData.builder()
                .matchId(matchId)
                .tournamentId(1L)
                .tournamentNameSnapshot(tournamentName)
                .seasonSnapshot("2026")
                .ourTeamId(1L)
                .ourTeamNameSnapshot("Our Team")
                .opponentTeamId(2L)
                .opponentTeamNameSnapshot("Opponent A")
                .matchTime(OffsetDateTime.parse("2026-05-20T20:00:00+02:00"))
                .homeAway("HOME")
                .ourScore(0)
                .opponentScore(0)
                .matchStatus("SCHEDULED")
                .finished(false)
                .build();
    }

    private MatchResultUpdatedEventData matchResultUpdated(Long matchId) {
        return MatchResultUpdatedEventData.builder()
                .matchId(matchId)
                .tournamentId(1L)
                .tournamentNameSnapshot("Spring Cup")
                .seasonSnapshot("2026")
                .ourTeamId(1L)
                .ourTeamNameSnapshot("Our Team")
                .opponentTeamId(2L)
                .opponentTeamNameSnapshot("Opponent A")
                .matchTime(OffsetDateTime.parse("2026-05-20T20:00:00+02:00"))
                .homeAway("HOME")
                .oldOurScore(0)
                .oldOpponentScore(0)
                .newOurScore(2)
                .newOpponentScore(1)
                .matchStatus("FINISHED")
                .finished(true)
                .build();
    }
}
