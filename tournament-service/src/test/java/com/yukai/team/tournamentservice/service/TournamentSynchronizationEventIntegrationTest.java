package com.yukai.team.tournamentservice.service;

import com.yukai.team.tournamentservice.config.JacksonConfig;
import com.yukai.team.tournamentservice.dto.request.UpdateTournamentRequest;
import com.yukai.team.tournamentservice.entity.Tournament;
import com.yukai.team.tournamentservice.entity.enums.TournamentStatus;
import com.yukai.team.tournamentservice.entity.enums.TournamentType;
import com.yukai.team.tournamentservice.outbox.OperatorProvider;
import com.yukai.team.tournamentservice.repository.OutboxEventRepository;
import com.yukai.team.tournamentservice.repository.TournamentRepository;
import com.yukai.team.tournamentservice.service.impl.OutboxEventServiceImpl;
import com.yukai.team.tournamentservice.service.impl.TournamentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = TournamentSynchronizationEventIntegrationTest.Config.class, properties = {
        "spring.datasource.url=${sync.test.jdbc-url:jdbc:h2:mem:tournament-sync;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS tournament}",
        "spring.datasource.driver-class-name=${sync.test.jdbc-driver:org.h2.Driver}",
        "spring.datasource.username=${sync.test.jdbc-user:sa}","spring.datasource.password=${sync.test.jdbc-password:}", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=tournament"})
class TournamentSynchronizationEventIntegrationTest {
    @Autowired TournamentRepository tournaments;
    @Autowired OutboxEventRepository outbox;
    @Autowired TournamentService service;
    @Autowired TransactionTemplate transactions;

    @BeforeEach
    void clean() { outbox.deleteAll(); tournaments.deleteAll(); }

    @ParameterizedTest
    @CsvSource({"Ligue B,2025/2026", "Ligue A,2026/2027", "Ligue B,2026/2027"})
    void eventContainsFlushedPersistedStateInTheOriginalEnvelope(String name, String season) {
        var original = tournaments.saveAndFlush(tournament(TournamentStatus.ACTIVE));
        service.updateTournament(original.getId(), request(name, season));
        var saved = tournaments.findById(original.getId()).orElseThrow();
        var row = outbox.findAll().get(0);
        var envelope = row.getPayloadJson();
        assertThat(saved.getVersion()).isEqualTo(original.getVersion() + 1);
        assertThat(envelope.path("eventType").asText()).isEqualTo("tournament.updated");
        assertThat(envelope.path("aggregateType").asText()).isEqualTo("tournament");
        assertThat(envelope.path("aggregateId").asLong()).isEqualTo(saved.getId());
        assertThat(envelope.path("eventId").asText()).isEqualTo(row.getId().toString());
        assertThat(envelope.path("data").path("name").asText()).isEqualTo(name);
        assertThat(envelope.path("data").path("season").asText()).isEqualTo(season);
        assertThat(envelope.path("data").path("version").asLong()).isEqualTo(saved.getVersion());
        assertThat(service.getTournamentSnapshot(saved.getId()).getVersion()).isEqualTo(saved.getVersion());
    }

    @Test
    void outerTransactionFailureRollsBackBothFlushedUpdateAndPublishableEvent() {
        var original = tournaments.saveAndFlush(tournament(TournamentStatus.ACTIVE));
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            service.updateTournament(original.getId(), request("New", "New season"));
            throw new IllegalStateException("abort transaction");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(outbox.count()).isZero();
        assertThat(tournaments.findById(original.getId()).orElseThrow().getName()).isEqualTo("Ligue A");
        assertThat(tournaments.findById(original.getId()).orElseThrow().getVersion()).isEqualTo(original.getVersion());
    }

    @Test
    void invalidDatesAndExistingStatusRestrictionsDoNotCreateEvents() {
        for (var status : new TournamentStatus[]{TournamentStatus.FINISHED, TournamentStatus.CANCELLED}) {
            var row = tournaments.saveAndFlush(tournament(status));
            assertThatThrownBy(() -> service.updateTournament(row.getId(), request("New", "New season"))).isInstanceOf(RuntimeException.class);
        }
        var active = tournaments.saveAndFlush(tournament(TournamentStatus.ACTIVE));
        var invalid = request("New", "New season");
        invalid.setStartDate(LocalDate.of(2026, 2, 1)); invalid.setEndDate(LocalDate.of(2026, 1, 1));
        assertThatThrownBy(() -> service.updateTournament(active.getId(), invalid)).isInstanceOf(RuntimeException.class);
        assertThat(outbox.count()).isZero();
    }

    @Test
    void staleJpaEntityStillFailsOptimisticLocking() {
        var original = tournaments.saveAndFlush(tournament(TournamentStatus.ACTIVE));
        service.updateTournament(original.getId(), request("New", "2026"));
        original.setName("Stale");
        assertThatThrownBy(() -> tournaments.saveAndFlush(original)).isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
        assertThat(outbox.count()).isEqualTo(1);
    }

    private Tournament tournament(TournamentStatus status) {
        return Tournament.builder().name("Ligue A").season("2025/2026").tournamentType(TournamentType.LEAGUE).status(status).build();
    }
    private UpdateTournamentRequest request(String name, String season) {
        var request = new UpdateTournamentRequest(); request.setName(name); request.setSeason(season); request.setTournamentType(TournamentType.LEAGUE); return request;
    }
    @Configuration
    @EnableAutoConfiguration
    @EntityScan("com.yukai.team.tournamentservice.entity")
    @EnableJpaRepositories("com.yukai.team.tournamentservice.repository")
    @Import({TournamentServiceImpl.class, OutboxEventServiceImpl.class, OperatorProvider.class, JacksonConfig.class})
    static class Config { }
}
