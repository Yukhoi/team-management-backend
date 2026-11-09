package com.yukai.team.matchservice.opponentanalysis.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yukai.team.matchservice.opponentanalysis.client.FlaClient;
import com.yukai.team.matchservice.opponentanalysis.config.FlaProperties;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchesPageResponse;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeamMatchesSnapshot;
import com.yukai.team.matchservice.opponentanalysis.service.CacheStatus;
import com.yukai.team.matchservice.opponentanalysis.service.FlaTeamMatchesSnapshotService;
import com.yukai.team.matchservice.opponentanalysis.service.FlaTeamMatchesSnapshotWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recent-matches;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;INIT=CREATE SCHEMA IF NOT EXISTS match",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.default_schema=match"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({FlaTeamMatchesSnapshotPersistenceTest.Config.class, FlaTeamMatchesSnapshotService.class, FlaTeamMatchesSnapshotWriter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FlaTeamMatchesSnapshotPersistenceTest {
    @Autowired private FlaTeamMatchesSnapshotService snapshots;
    @Autowired private FlaTeamMatchesSnapshotRepository repository;
    @Autowired private FlaTeamMatchesSnapshotWriter writer;
    @MockitoBean private FlaClient client;

    @BeforeEach
    void clearSnapshots() { repository.deleteAll(); }

    @Test
    void durableVersionsAreIsolatedByTeamAndReusedWithoutOverwritingHistory() {
        when(client.getTeamMatchesPage(270L)).thenReturn(new FlaTeamMatchesPageResponse(270L, 16L, null, List.of()));
        var first = snapshots.getTeamMatches(270L, false);
        assertThat(first.cacheStatus()).isEqualTo(CacheStatus.MISS);
        assertThat(snapshots.getTeamMatches(270L, false).cacheStatus()).isEqualTo(CacheStatus.HIT);
        verify(client).getTeamMatchesPage(270L);
        assertThat(snapshots.getTeamMatches(270L, true).cacheStatus()).isEqualTo(CacheStatus.REUSED_IDENTICAL_PAYLOAD);
        when(client.getTeamMatchesPage(63L)).thenReturn(new FlaTeamMatchesPageResponse(63L, 16L, null, List.of()));
        var other = snapshots.getTeamMatches(63L, false);
        assertThat(other.payloadHash()).isNotEqualTo(first.payloadHash());
        when(client.getTeamMatchesPage(270L)).thenReturn(new FlaTeamMatchesPageResponse(270L, 16L, 2L, List.of()));
        var changed = snapshots.getTeamMatches(270L, true);
        assertThat(changed.cacheStatus()).isEqualTo(CacheStatus.REFRESHED);
        assertThat(changed.payloadHash()).isNotEqualTo(first.payloadHash());
        assertThat(repository.findById(first.payloadHash())).isPresent();
        assertThat(repository.count()).isEqualTo(3);
        assertThat(snapshots.getTeamMatches(270L, false).page().saisonId()).isEqualTo(2L);
    }

    @Test
    void duplicateInsertRollsBackBeforeTouchAndCannotOverwritePayloadOrDecreaseFreshness() {
        when(client.getTeamMatchesPage(270L)).thenReturn(new FlaTeamMatchesPageResponse(270L, 16L, null, List.of()));
        var first = snapshots.getTeamMatches(270L, false);
        var existing = repository.findById(first.payloadHash()).orElseThrow();
        FlaTeamMatchesSnapshot duplicate = new FlaTeamMatchesSnapshot();
        duplicate.setPayloadHash(first.payloadHash());
        duplicate.setFlaTeamId(270L);
        duplicate.setRawPayload("corrupt replacement");
        duplicate.setFetchedAt(OffsetDateTime.now());
        assertThatThrownBy(() -> writer.insert(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
        var after = writer.touch(first.payloadHash(), existing.getFetchedAt().minusDays(1));
        assertThat(after.getRawPayload()).isEqualTo(existing.getRawPayload());
        assertThat(after.getFetchedAt()).isEqualTo(existing.getFetchedAt());
        assertThat(repository.count()).isEqualTo(1);
    }

    @TestConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = FlaTeamMatchesSnapshot.class)
    @EnableJpaRepositories(basePackageClasses = FlaTeamMatchesSnapshotRepository.class)
    static class Config {
        @Bean FlaProperties properties() { return new FlaProperties(); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().registerModule(new JavaTimeModule()); }
    }
}
