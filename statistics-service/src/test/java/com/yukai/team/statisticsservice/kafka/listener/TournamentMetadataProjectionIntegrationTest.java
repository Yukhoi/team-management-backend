package com.yukai.team.statisticsservice.kafka.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.statisticsservice.dto.event.StatisticsEventEnvelope;
import com.yukai.team.statisticsservice.entity.*;
import com.yukai.team.statisticsservice.entity.enums.*;
import com.yukai.team.statisticsservice.repository.*;
import com.yukai.team.statisticsservice.service.cache.StatisticsCacheInvalidationService;
import com.yukai.team.statisticsservice.service.projection.LeaderboardProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={
        "spring.datasource.url=${sync.test.jdbc-url:jdbc:h2:mem:tournament-statistics;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH}",
        "spring.datasource.driver-class-name=${sync.test.jdbc-driver:org.h2.Driver}","spring.datasource.username=${sync.test.jdbc-user:sa}","spring.datasource.password=${sync.test.jdbc-password:}",
        "spring.jpa.hibernate.ddl-auto=validate","spring.sql.init.mode=always","spring.kafka.listener.auto-startup=false"})
class TournamentMetadataProjectionIntegrationTest {
    @Autowired MatchStatisticsKafkaListener listener;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired MatchSummaryProjectionRepository summaries;
    @Autowired PlayerStatsProjectionRepository players;
    @Autowired TeamStatsProjectionRepository teams;
    @Autowired LeaderboardProjectionRepository boards;
    @Autowired MatchPlayerAppearanceProjectionRepository appearances;
    @MockitoBean StatisticsCacheInvalidationService caches;
    @MockitoSpyBean LeaderboardProjectionService leaderboards;

    @BeforeEach
    void clean() {
        reset(leaderboards,caches);
        for (String table : new String[]{"event_consume_record","tournament_metadata_state","match_summary_projection","player_stats_projection","team_stats_projection","leaderboard_projection","match_player_appearance_projection"}) jdbc.update("DELETE FROM statistics."+table);
    }
    @Test
    void metadataRefreshPreservesAllCountersAndBlocksOlderMetadataAndLateMatchSnapshots() throws Exception {
        seed("2025");
        var ack = mock(Acknowledgment.class);
        listener.onMessage(metadata("Latest","2027",3),ack);
        verify(ack).acknowledge();
        assertThat(players.findByPlayerIdAndSeasonAndTournamentId(9L,"2027",5L)).hasValueSatisfying(row -> {
            assertThat(row.getGoals()).isEqualTo(3); assertThat(row.getAssists()).isEqualTo(2);
            assertThat(row.getAppearances()).isEqualTo(4); assertThat(row.getStarts()).isEqualTo(2);
            assertThat(row.getTournamentNameSnapshot()).isEqualTo("Latest");
        });
        assertThat(teams.findByTeamIdAndSeasonAndTournamentId(1L,"2027",5L)).hasValueSatisfying(row -> {
            assertThat(row.getGoalsFor()).isEqualTo(4); assertThat(row.getPoints()).isEqualTo(3);
        });
        assertThat(summaries.findByMatchId(11L).orElseThrow().getSeasonSnapshot()).isEqualTo("2027");
        assertThat(summaries.findByMatchId(11L).orElseThrow().getOurScore()).isEqualTo(4);
        assertThat(appearances.findByMatchId(11L).get(0).getSeason()).isEqualTo("2027");
        assertThat(boards.findAll().get(0).getMetricValue()).isEqualTo(3);
        assertThat(boards.findAll().get(0).getSeason()).isEqualTo("2027");
        listener.onMessage(metadata("Old","2025",1),mock(Acknowledgment.class));
        // Replay a stale match.created from a different partition after the metadata event.
        var stale = mapper.createObjectNode(); stale.put("matchId",11); stale.put("tournamentId",5);
        stale.put("tournamentNameSnapshot","Old"); stale.put("seasonSnapshot","2025");
        stale.put("matchTime","2020-01-01T18:00:00Z"); stale.put("opponentTeamNameSnapshot","Opponent"); stale.put("homeAway","HOME"); stale.put("matchStatus","FINISHED"); stale.put("finished",true); stale.put("ourScore",4); stale.put("opponentScore",2);
        listener.onMessage(envelope("match.created","match",11L,stale),mock(Acknowledgment.class));
        assertThat(summaries.findByMatchId(11L).orElseThrow().getTournamentNameSnapshot()).isEqualTo("Latest");
        assertThat(summaries.findByMatchId(11L).orElseThrow().getSeasonSnapshot()).isEqualTo("2027");
        verify(leaderboards,never()).rebuildLeaderboards(any(),any());
        verify(caches,atLeastOnce()).evictStatisticsReadCaches();
    }
    @Test
    void splitSeasonBucketsAreConsolidatedWithoutLosingGoalsAssistsOrAppearances() throws Exception {
        seed("2025");
        players.saveAndFlush(player("2026"));
        teams.saveAndFlush(team("2026"));
        listener.onMessage(metadata("Latest","2027",3),mock(Acknowledgment.class));
        assertThat(players.findAll()).hasSize(1);
        var player = players.findAll().get(0);
        assertThat(player.getGoals()).isEqualTo(6); assertThat(player.getAssists()).isEqualTo(4);
        assertThat(player.getAppearances()).isEqualTo(8); assertThat(player.getStarts()).isEqualTo(4);
        assertThat(teams.findAll()).hasSize(1);
        assertThat(teams.findAll().get(0).getGoalsFor()).isEqualTo(8);
        assertThat(teams.findAll().get(0).getPlayed()).isEqualTo(2);
        verify(leaderboards).rebuildLeaderboards("2027",5L);
        listener.onMessage(metadata("Latest","2027",3),mock(Acknowledgment.class));
        assertThat(players.findAll().get(0).getGoals()).isEqualTo(6);
    }
    @Test
    void failedProjectionRollsBackMetadataCountersConsumeRecordAndDoesNotAcknowledge() throws Exception {
        seed("2025"); players.saveAndFlush(player("2026"));
        doThrow(new IllegalStateException("projection failure")).when(leaderboards).rebuildLeaderboards(any(),any());
        var ack = mock(Acknowledgment.class);
        assertThatThrownBy(() -> listener.onMessage(metadata("Latest","2027",3),ack)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(ack);
        assertThat(players.findAll()).hasSize(2);
        assertThat(summaries.findByMatchId(11L).orElseThrow().getSeasonSnapshot()).isEqualTo("2025");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM statistics.tournament_metadata_state",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM statistics.event_consume_record",Integer.class)).isZero();
    }
    @Test
    void manualMigrationPreservesAppliedVersionWhenRepeated() throws Exception {
        listener.onMessage(metadata("Latest","2027",3),mock(Acknowledgment.class));
        var script = new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("db/migration/V8__create_tournament_metadata_state.sql"));
        script.execute(jdbc.getDataSource()); script.execute(jdbc.getDataSource());
        assertThat(jdbc.queryForObject("SELECT last_applied_version FROM statistics.tournament_metadata_state WHERE tournament_id=5",Long.class)).isEqualTo(3);
    }

    private void seed(String season) {
        summaries.saveAndFlush(MatchSummaryProjection.builder().matchId(11L).tournamentId(5L).tournamentNameSnapshot("Old").seasonSnapshot(season)
                .matchTime(OffsetDateTime.parse("2020-01-01T18:00:00Z")).opponentTeamNameSnapshot("Opponent").homeAway(HomeAwayType.HOME)
                .ourScore(4).opponentScore(2).matchStatus(MatchStatus.FINISHED).finished(true).build());
        players.saveAndFlush(player(season)); teams.saveAndFlush(team(season));
        appearances.saveAndFlush(MatchPlayerAppearanceProjection.builder().matchId(11L).playerId(9L).tournamentId(5L).season(season).appearanceCount(1).starterCount(1).build());
        boards.saveAndFlush(LeaderboardProjection.builder().boardType(LeaderboardBoardType.SCORER).season(season).tournamentId(5L).rankNo(1).entityId(9L).entityNameSnapshot("Player").metricValue(3).appearanceCount(4).build());
    }
    private PlayerStatsProjection player(String season) { return PlayerStatsProjection.builder().playerId(9L).playerNameSnapshot("Player").season(season).tournamentId(5L).tournamentNameSnapshot("Old").goals(3).assists(2).goalInvolvements(5).appearances(4).starts(2).build(); }
    private TeamStatsProjection team(String season) { return TeamStatsProjection.builder().teamId(1L).teamNameSnapshot("Team").season(season).tournamentId(5L).tournamentNameSnapshot("Old").played(1).win(1).goalsFor(4).goalsAgainst(2).goalDiff(2).points(3).rankNo(1).build(); }
    private String metadata(String name,String season,long version) throws Exception { return envelope("tournament.metadata.synchronized","tournament",5L,Map.of("tournamentId",5,"name",name,"season",season,"version",version)); }
    private String envelope(String type,String aggregate,long id,Object data) throws Exception { return mapper.writeValueAsString(Map.of("eventId",UUID.randomUUID(),"eventType",type,"aggregateType",aggregate,"aggregateId",id,"occurredAt","2026-11-10T12:00:00Z","data",data)); }
}
