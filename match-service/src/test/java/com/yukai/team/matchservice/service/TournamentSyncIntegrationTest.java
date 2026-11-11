package com.yukai.team.matchservice.service;

import com.yukai.team.matchservice.config.JacksonConfig;
import com.yukai.team.matchservice.dto.response.TournamentSnapshotResponse;
import com.yukai.team.matchservice.entity.*;
import com.yukai.team.matchservice.outbox.OperatorProvider;
import com.yukai.team.matchservice.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.OffsetDateTime;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = TournamentSyncIntegrationTest.Config.class, properties = {
        "spring.datasource.url=${sync.test.jdbc-url:jdbc:h2:mem:match-tournament-sync;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS match}",
        "spring.datasource.driver-class-name=${sync.test.jdbc-driver:org.h2.Driver}",
        "spring.datasource.username=${sync.test.jdbc-user:sa}","spring.datasource.password=${sync.test.jdbc-password:}", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=match"})
class TournamentSyncIntegrationTest {
    @Autowired TournamentSyncService sync;
    @Autowired MatchInfoRepository matches;
    @Autowired MatchAppearanceRepository appearances;
    @Autowired OutboxEventRepository events;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;
    @Autowired EntityManager em;
    @MockitoSpyBean OutboxEventServiceImpl outbox;

    @BeforeEach
    void clean() {
        reset(outbox);
        jdbc.execute("CREATE TABLE IF NOT EXISTS match.tournament_sync_state(tournament_id BIGINT PRIMARY KEY,last_applied_version BIGINT NOT NULL,name VARCHAR(100),season VARCHAR(30),updated_at TIMESTAMP WITH TIME ZONE NOT NULL)");
        appearances.deleteAll(); matches.deleteAll(); events.deleteAll(); jdbc.update("DELETE FROM match.tournament_sync_state");
    }

    @Test
    void updatesEveryStatusIncludingHistoricalMatchesWithoutChangingOtherData() {
        for (var status : MatchStatus.values()) matches.saveAndFlush(match(5L, status));
        var unrelated = matches.saveAndFlush(match(6L, MatchStatus.FINISHED));
        var historic = matches.saveAndFlush(match(5L, MatchStatus.FINISHED));
        var appearance = new MatchAppearance(); appearance.setMatch(historic); appearance.setPlayerId(9L); appearance.setPlayerNameSnapshot("Player"); appearance.setAppeared(true); appearance.setStarter(true);
        appearances.saveAndFlush(appearance);
        var before = matches.findAll();
        sync.synchronizeTournament(5L, "Ligue B", "2026/2027", 2L);
        for (var previous : before) {
            var current = matches.findById(previous.getId()).orElseThrow();
            assertThat(current).usingRecursiveComparison().ignoringFields("tournamentNameSnapshot", "seasonSnapshot", "updatedAt", "version").isEqualTo(previous);
            assertThat(current.getTournamentNameSnapshot()).isEqualTo(current.getTournamentId().equals(5L) ? "Ligue B" : "Ligue A");
            assertThat(current.getSeasonSnapshot()).isEqualTo(current.getTournamentId().equals(5L) ? "2026/2027" : "2025/2026");
            assertThat(current.getVersion()).isEqualTo(previous.getVersion() + (current.getTournamentId().equals(5L) ? 1 : 0));
        }
        assertThat(appearances.findAll()).hasSize(1);
        assertThat(appearances.findById(appearance.getId()).orElseThrow().getVersion()).isEqualTo(appearance.getVersion());
        assertThat(matches.findById(unrelated.getId()).orElseThrow().getVersion()).isEqualTo(unrelated.getVersion());
        assertThat(events.count()).isEqualTo(1);
    }

    @Test
    void duplicateStaleAndEqualVersionsNeverRegressOrRewriteCorrectRows() {
        var row = matches.saveAndFlush(match(5L, MatchStatus.FINISHED));
        sync.synchronizeTournament(5L, "Latest", "2027", 3L);
        long version = matches.findById(row.getId()).orElseThrow().getVersion();
        sync.synchronizeTournament(5L, "Latest", "2027", 3L);
        sync.synchronizeTournament(5L, "Old", "2025", 1L);
        assertThatThrownBy(() -> sync.synchronizeTournament(5L, "Conflict", "2027", 3L)).isInstanceOf(IllegalArgumentException.class);
        assertThat(matches.findById(row.getId()).orElseThrow().getVersion()).isEqualTo(version);
        assertThat(matches.findById(row.getId()).orElseThrow().getTournamentNameSnapshot()).isEqualTo("Latest");
        assertThat(events.count()).isEqualTo(1);
        sync.synchronizeTournament(5L, "Latest", "2027", 4L);
        assertThat(matches.findById(row.getId()).orElseThrow().getVersion()).isEqualTo(version);
    }

    @Test
    void failureAfterBulkUpdateRollsBackMatchesStateAndOutbox() {
        var row = matches.saveAndFlush(match(5L, MatchStatus.FINISHED));
        doThrow(new IllegalStateException("outbox unavailable")).when(outbox).saveEvent(any(), any(), any(), any());
        assertThatThrownBy(() -> sync.synchronizeTournament(5L, "New", "2027", 1L)).isInstanceOf(IllegalStateException.class);
        assertThat(matches.findById(row.getId()).orElseThrow().getTournamentNameSnapshot()).isEqualTo("Ligue A");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match.tournament_sync_state", Integer.class)).isZero();
        assertThat(events.count()).isZero();
        reset(outbox);
        sync.synchronizeTournament(5L, "New", "2027", 1L);
        doThrow(new IllegalStateException("outbox unavailable")).when(outbox).saveEvent(any(), any(), any(), any());
        assertThatThrownBy(() -> sync.synchronizeTournament(5L, "Newer", "2028", 2L)).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT last_applied_version FROM match.tournament_sync_state WHERE tournament_id=5", Long.class)).isEqualTo(1);
        assertThat(matches.findById(row.getId()).orElseThrow().getTournamentNameSnapshot()).isEqualTo("New");
    }

    @Test
    void reconciliationRepairsEqualVersionDriftAndCannotRegressNewerEvents() {
        var row = matches.saveAndFlush(match(5L, MatchStatus.FINISHED));
        sync.synchronizeTournament(5L, "Latest", "2027", 3L);
        jdbc.update("UPDATE match.match_info SET tournament_name_snapshot='Drift' WHERE id=?", row.getId());
        sync.reconcileTournament(snapshot("Latest", "2027", 3L));
        sync.reconcileTournament(snapshot("Stale HTTP response", "2025", 1L));
        assertThat(matches.findById(row.getId()).orElseThrow().getTournamentNameSnapshot()).isEqualTo("Latest");
        assertThat(events.count()).isEqualTo(2);
    }

    @Test
    void concurrentFirstEventsAndReconciliationHaveOneMonotonicDurableState() throws Exception {
        matches.saveAndFlush(match(5L, MatchStatus.FINISHED));
        var executor = Executors.newFixedThreadPool(3);
        var start = new CountDownLatch(1);
        try {
            var a = executor.submit(() -> { await(start); sync.synchronizeTournament(5L, "V1", "2025", 1L); });
            var b = executor.submit(() -> { await(start); sync.synchronizeTournament(5L, "V3", "2027", 3L); });
            var c = executor.submit(() -> { await(start); sync.reconcileTournament(snapshot("V2", "2026", 2L)); });
            start.countDown(); a.get(15, TimeUnit.SECONDS); b.get(15, TimeUnit.SECONDS); c.get(15, TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject("SELECT last_applied_version FROM match.tournament_sync_state WHERE tournament_id=5", Long.class)).isEqualTo(3);
            assertThat(matches.findAll().get(0).getTournamentNameSnapshot()).isEqualTo("V3");
        } finally { executor.shutdownNow(); }
    }

    @Test
    void creationUsesLatestAppliedMetadataAndHoldsLockThroughInsert() {
        sync.synchronizeTournament(5L, "Latest", "2027", 3L);
        tx.executeWithoutResult(status -> {
            var resolved = sync.prepareForCreation(snapshot("Old HTTP", "2025", 1L));
            assertThat(resolved.getName()).isEqualTo("Latest");
            var row = match(5L, MatchStatus.SCHEDULED); row.setTournamentNameSnapshot(resolved.getName()); row.setSeasonSnapshot(resolved.getSeason()); matches.saveAndFlush(row);
        });
        assertThat(matches.findAll().get(0).getTournamentNameSnapshot()).isEqualTo("Latest");
    }

    @Test
    void staleManagedMatchCannotOverwriteSynchronizedMetadata() {
        var row = matches.saveAndFlush(match(5L, MatchStatus.SCHEDULED));
        sync.synchronizeTournament(5L, "Latest", "2027", 3L);
        row.setVenue("Changed concurrently");
        assertThatThrownBy(() -> matches.saveAndFlush(row)).isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
        assertThat(matches.findById(row.getId()).orElseThrow().getTournamentNameSnapshot()).isEqualTo("Latest");
    }

    @Test
    void manualMigrationIsRepeatableAndPreservesAppliedProgress() {
        sync.synchronizeTournament(5L,"Latest","2027",3L);
        var path = java.nio.file.Path.of("sql/create-tournament-sync-state.sql");
        if (!java.nio.file.Files.exists(path)) path = java.nio.file.Path.of("match-service").resolve(path);
        var script = new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(new org.springframework.core.io.FileSystemResource(path));
        script.execute(jdbc.getDataSource()); script.execute(jdbc.getDataSource());
        assertThat(jdbc.queryForObject("SELECT last_applied_version FROM match.tournament_sync_state WHERE tournament_id=5",Long.class)).isEqualTo(3);
    }

    private static void await(CountDownLatch start) { try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); } }
    static TournamentSnapshotResponse snapshot(String name, String season, long version) {
        var value = new TournamentSnapshotResponse(); value.setId(5L); value.setName(name); value.setSeason(season); value.setVersion(version); value.setStatus("ACTIVE"); return value;
    }
    static MatchInfo match(Long id, MatchStatus status) {
        var row = new MatchInfo(); row.setTournamentId(id); row.setTournamentNameSnapshot("Ligue A"); row.setSeasonSnapshot("2025/2026");
        row.setOurTeamId(1L); row.setOurTeamNameSnapshot("Our team"); row.setOpponentTeamId(2L); row.setOpponentTeamNameSnapshot("Opponent");
        row.setMatchTime(OffsetDateTime.parse("2020-01-01T18:00:00Z")); row.setHomeAway(HomeAway.HOME); row.setMatchStatus(status);
        row.setOurScore(4); row.setOpponentScore(2); row.setFinished(status==MatchStatus.FINISHED); return row;
    }
    @Configuration
    @EnableAutoConfiguration
    @EntityScan("com.yukai.team.matchservice.entity")
    @EnableJpaRepositories("com.yukai.team.matchservice.repository")
    @Import({TournamentSyncServiceImpl.class,TournamentSyncStateRepository.class,OutboxEventServiceImpl.class,OperatorProvider.class,JacksonConfig.class})
    static class Config { }
}
