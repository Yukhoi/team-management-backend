package com.yukai.team.matchservice.opponentanalysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yukai.team.matchservice.opponentanalysis.client.FlaClient;
import com.yukai.team.matchservice.opponentanalysis.config.FlaProperties;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchesPageResponse;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeamMatchesSnapshot;
import com.yukai.team.matchservice.opponentanalysis.exception.SnapshotProcessingException;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaTeamMatchesSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlaTeamMatchesSnapshotServiceTest {
    @Mock private FlaTeamMatchesSnapshotRepository repository;
    @Mock private FlaTeamMatchesSnapshotWriter writer;
    @Mock private FlaClient client;
    private FlaProperties properties;
    private FlaTeamMatchesSnapshotService service;

    @BeforeEach
    void setUp() {
        properties = new FlaProperties();
        service = service();
    }

    @Test
    void canonicalHashSurvivesOrderChangesAndServiceRestartAndSeparatesTeams() throws IOException {
        var matches = new ArrayList<>(RecentFormCalculatorTest.fixture(270));
        var first = service.canonical(new FlaTeamMatchesPageResponse(270L, 16L, null, matches));
        Collections.reverse(matches);
        assertThat(service().canonical(new FlaTeamMatchesPageResponse(270L, 16L, null, matches))).isEqualTo(first);
        assertThat(service.canonical(new FlaTeamMatchesPageResponse(63L, 16L, null, matches)).hash()).isNotEqualTo(first.hash());
        assertThat(service.canonical(new FlaTeamMatchesPageResponse(270L, 16L, 2L, matches)).hash()).isNotEqualTo(first.hash());
        matches.remove(0);
        assertThat(service.canonical(new FlaTeamMatchesPageResponse(270L, 16L, null, matches)).hash()).isNotEqualTo(first.hash());
    }

    @Test
    void missInsertsIndependentVersionAndHitAvoidsHttp() {
        var page = page();
        when(client.getTeamMatchesPage(270L)).thenReturn(page);
        var miss = service.getTeamMatches(270L, false);
        assertThat(miss.cacheStatus()).isEqualTo(CacheStatus.MISS);
        verify(writer).insert(any());
        FlaTeamMatchesSnapshot saved = snapshot(page);
        when(repository.findTopByFlaTeamIdOrderByFetchedAtDescPayloadHashDesc(270L)).thenReturn(Optional.of(saved));
        var hit = service.getTeamMatches(270L, false);
        assertThat(hit.cacheStatus()).isEqualTo(CacheStatus.HIT);
        assertThat(hit.page()).isEqualTo(page);
        verify(client).getTeamMatchesPage(270L); // only the MISS fetched
    }

    @Test
    void forceRefreshIdenticalPayloadReusesDurableVersionAndTouchesTime() {
        var saved = snapshot(page());
        when(repository.findTopByFlaTeamIdOrderByFetchedAtDescPayloadHashDesc(270L)).thenReturn(Optional.of(saved));
        when(client.getTeamMatchesPage(270L)).thenReturn(page());
        when(repository.findById(saved.getPayloadHash())).thenReturn(Optional.of(saved));
        when(writer.touch(eq(saved.getPayloadHash()), any())).thenReturn(saved);
        var result = service.getTeamMatches(270L, true);
        assertThat(result.cacheStatus()).isEqualTo(CacheStatus.REUSED_IDENTICAL_PAYLOAD);
        assertThat(result.payloadHash()).isEqualTo(saved.getPayloadHash());
        assertThat(result.forceRefreshRequested()).isTrue();
        verify(client).getTeamMatchesPage(270L);
    }

    @Test
    void expiredChangedPayloadIsRefreshedAndOldVersionRemainsIndependent() throws IOException {
        var saved = snapshot(page());
        saved.setFetchedAt(RecentFormCalculatorTest.FETCHED.minusYears(1));
        when(repository.findTopByFlaTeamIdOrderByFetchedAtDescPayloadHashDesc(270L)).thenReturn(Optional.of(saved));
        when(client.getTeamMatchesPage(270L)).thenReturn(new FlaTeamMatchesPageResponse(270L, 16L, null, RecentFormCalculatorTest.fixture(270)));
        var result = service.getTeamMatches(270L, false);
        assertThat(result.cacheStatus()).isEqualTo(CacheStatus.REFRESHED);
        assertThat(result.payloadHash()).isNotEqualTo(saved.getPayloadHash());
        assertThat(saved.getRawPayload()).isEqualTo(service.canonical(page()).json());
    }

    @Test
    void forceRefreshWithoutExistingSnapshotIsRefreshed() {
        when(client.getTeamMatchesPage(270L)).thenReturn(page());
        assertThat(service.getTeamMatches(270L, true).cacheStatus()).isEqualTo(CacheStatus.REFRESHED);
    }

    @Test
    void concurrentInsertConflictReloadsOnlyAfterWriterRollback() {
        var saved = snapshot(page());
        when(client.getTeamMatchesPage(270L)).thenReturn(page());
        when(repository.findById(saved.getPayloadHash())).thenReturn(Optional.empty(), Optional.of(saved));
        doThrow(new DataIntegrityViolationException("race")).when(writer).insert(any());
        when(writer.touch(eq(saved.getPayloadHash()), any())).thenReturn(saved);
        assertThat(service.getTeamMatches(270L, false).cacheStatus()).isEqualTo(CacheStatus.REUSED_IDENTICAL_PAYLOAD);
    }

    @Test
    void corruptedCacheAndWrongTeamIdentityAreNotEmptyLists() {
        var saved = snapshot(page());
        saved.setRawPayload("{}");
        when(repository.findTopByFlaTeamIdOrderByFetchedAtDescPayloadHashDesc(270L)).thenReturn(Optional.of(saved));
        assertThatThrownBy(() -> service.getTeamMatches(270L, false)).isInstanceOf(SnapshotProcessingException.class);
        saved.setFetchedAt(RecentFormCalculatorTest.FETCHED.minusYears(1));
        when(client.getTeamMatchesPage(270L)).thenReturn(new FlaTeamMatchesPageResponse(63L, 16L, null, List.of()));
        assertThatThrownBy(() -> service.getTeamMatches(270L, false)).isInstanceOf(SnapshotProcessingException.class);
    }

    @Test
    void separateTtlDoesNotChangeStandingsTtl() {
        properties.setRecentMatchesTtl(Duration.ofMinutes(5));
        when(client.getTeamMatchesPage(270L)).thenReturn(page());
        assertThat(service.getTeamMatches(270L, false).cacheTtlSeconds()).isEqualTo(300);
        assertThat(properties.getSnapshotTtl()).isEqualTo(Duration.ofHours(6));
    }

    @Test
    void rejectsInvalidTeamWithoutCacheOrHttpAccess() {
        for (Long id : new Long[]{null, 0L, -1L})
            assertThatThrownBy(() -> service.getTeamMatches(id, false)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository, client, writer);
    }

    private FlaTeamMatchesPageResponse page() { return new FlaTeamMatchesPageResponse(270L, 16L, null, List.of()); }

    private FlaTeamMatchesSnapshot snapshot(FlaTeamMatchesPageResponse page) {
        var payload = service.canonical(page);
        var saved = new FlaTeamMatchesSnapshot();
        saved.setPayloadHash(payload.hash());
        saved.setRawPayload(payload.json());
        saved.setFlaTeamId(page.flaTeamId());
        saved.setSourceSaisonId(page.saisonId());
        saved.setFetchedAt(java.time.OffsetDateTime.now());
        return saved;
    }

    private FlaTeamMatchesSnapshotService service() {
        return new FlaTeamMatchesSnapshotService(repository, writer, client, properties,
                new ObjectMapper().registerModule(new JavaTimeModule()));
    }
}
