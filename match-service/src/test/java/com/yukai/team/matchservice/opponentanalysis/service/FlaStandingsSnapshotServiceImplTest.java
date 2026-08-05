package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.config.JacksonConfig;
import com.yukai.team.matchservice.opponentanalysis.client.FlaClient;
import com.yukai.team.matchservice.opponentanalysis.config.FlaProperties;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.entity.ExternalDataSnapshot;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaClientException;
import com.yukai.team.matchservice.opponentanalysis.repository.ExternalDataSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlaStandingsSnapshotServiceImplTest {

    @Mock
    private ExternalDataSnapshotRepository externalDataSnapshotRepository;
    @Mock
    private ExternalDataSnapshotWriter externalDataSnapshotWriter;
    @Mock
    private FlaClient flaClient;

    @Test
    void usesFreshSnapshotWithoutCallingFlaClient() {
        ExternalDataSnapshot snapshot = snapshot(10L, OffsetDateTime.now(), rawPayload(entry(6580L, 20)), payloadHash());
        when(externalDataSnapshotRepository.findTopByProviderAndChampionnatIdAndSaisonIdOrderByFetchedAtDescIdDesc("FLA", 1365L, 15L))
                .thenReturn(Optional.of(snapshot));

        StandingsSnapshotResult result = service().getStandings(1365L, 15L, false);

        assertThat(result.snapshotId()).isEqualTo(10L);
        assertThat(result.cacheStatus()).isEqualTo(CacheStatus.HIT);
        assertThat(result.standings()).hasSize(1);
        verify(flaClient, never()).getStandings(any(), any());
    }

    @Test
    void callsFlaClientAndSavesWhenSnapshotIsMissing() {
        List<FlaStandingEntryResponse> standings = List.of(entry(6580L, 20));
        when(externalDataSnapshotRepository.findTopByProviderAndChampionnatIdAndSaisonIdOrderByFetchedAtDescIdDesc("FLA", 1365L, 15L))
                .thenReturn(Optional.empty());
        when(flaClient.getStandings(1365L, 15L)).thenReturn(standings);
        when(externalDataSnapshotWriter.saveOrUpdateFetchedAt(eq("FLA"), eq(1365L), eq(15L), any(), any(), any()))
                .thenReturn(new ExternalDataSnapshotWriter.SnapshotWriteResult(snapshot(11L), false));

        StandingsSnapshotResult result = service().getStandings(1365L, 15L, false);

        assertThat(result.cacheStatus()).isEqualTo(CacheStatus.MISS);
        assertThat(result.snapshotId()).isEqualTo(11L);
        verify(flaClient).getStandings(1365L, 15L);
    }

    @Test
    void refreshesWhenSnapshotIsExpired() {
        when(externalDataSnapshotRepository.findTopByProviderAndChampionnatIdAndSaisonIdOrderByFetchedAtDescIdDesc("FLA", 1365L, 15L))
                .thenReturn(Optional.of(snapshot(10L, OffsetDateTime.now().minusHours(7), rawPayload(entry(6580L, 10)), payloadHash())));
        when(flaClient.getStandings(1365L, 15L)).thenReturn(List.of(entry(6580L, 20)));
        when(externalDataSnapshotWriter.saveOrUpdateFetchedAt(eq("FLA"), eq(1365L), eq(15L), any(), any(), any()))
                .thenReturn(new ExternalDataSnapshotWriter.SnapshotWriteResult(snapshot(11L), false));

        StandingsSnapshotResult result = service().getStandings(1365L, 15L, false);

        assertThat(result.cacheStatus()).isEqualTo(CacheStatus.REFRESHED);
        verify(flaClient).getStandings(1365L, 15L);
    }

    @Test
    void forceRefreshBypassesFreshSnapshot() {
        when(externalDataSnapshotRepository.findTopByProviderAndChampionnatIdAndSaisonIdOrderByFetchedAtDescIdDesc("FLA", 1365L, 15L))
                .thenReturn(Optional.of(snapshot(10L, OffsetDateTime.now(), rawPayload(entry(6580L, 10)), payloadHash())));
        when(flaClient.getStandings(1365L, 15L)).thenReturn(List.of(entry(6580L, 20)));
        when(externalDataSnapshotWriter.saveOrUpdateFetchedAt(eq("FLA"), eq(1365L), eq(15L), any(), any(), any()))
                .thenReturn(new ExternalDataSnapshotWriter.SnapshotWriteResult(snapshot(11L), false));

        StandingsSnapshotResult result = service().getStandings(1365L, 15L, true);

        assertThat(result.cacheStatus()).isEqualTo(CacheStatus.REFRESHED);
        verify(flaClient).getStandings(1365L, 15L);
    }

    @Test
    void reusesIdenticalPayloadAndDoesNotCreateDuplicateSnapshot() {
        when(externalDataSnapshotRepository.findTopByProviderAndChampionnatIdAndSaisonIdOrderByFetchedAtDescIdDesc("FLA", 1365L, 15L))
                .thenReturn(Optional.of(snapshot(10L, OffsetDateTime.now().minusHours(7), rawPayload(entry(6580L, 20)), payloadHash())));
        when(flaClient.getStandings(1365L, 15L)).thenReturn(List.of(entry(6580L, 20)));
        when(externalDataSnapshotWriter.saveOrUpdateFetchedAt(eq("FLA"), eq(1365L), eq(15L), any(), any(), any()))
                .thenReturn(new ExternalDataSnapshotWriter.SnapshotWriteResult(snapshot(10L), true));

        StandingsSnapshotResult result = service().getStandings(1365L, 15L, false);

        assertThat(result.cacheStatus()).isEqualTo(CacheStatus.REUSED_IDENTICAL_PAYLOAD);
        assertThat(result.snapshotId()).isEqualTo(10L);
    }

    @Test
    void treatsCorruptedCachedPayloadAsUnavailableAndRefreshes() {
        when(externalDataSnapshotRepository.findTopByProviderAndChampionnatIdAndSaisonIdOrderByFetchedAtDescIdDesc("FLA", 1365L, 15L))
                .thenReturn(Optional.of(snapshot(10L, OffsetDateTime.now(), "not-json", payloadHash())));
        when(flaClient.getStandings(1365L, 15L)).thenReturn(List.of(entry(6580L, 20)));
        when(externalDataSnapshotWriter.saveOrUpdateFetchedAt(eq("FLA"), eq(1365L), eq(15L), any(), any(), any()))
                .thenReturn(new ExternalDataSnapshotWriter.SnapshotWriteResult(snapshot(11L), false));

        StandingsSnapshotResult result = service().getStandings(1365L, 15L, false);

        assertThat(result.cacheStatus()).isEqualTo(CacheStatus.REFRESHED);
        verify(flaClient).getStandings(1365L, 15L);
    }

    @Test
    void propagatesFlaClientException() {
        when(externalDataSnapshotRepository.findTopByProviderAndChampionnatIdAndSaisonIdOrderByFetchedAtDescIdDesc("FLA", 1365L, 15L))
                .thenReturn(Optional.empty());
        when(flaClient.getStandings(1365L, 15L))
                .thenThrow(new FlaClientException(HttpStatus.BAD_GATEWAY, "FLA_UNAVAILABLE", "FLA service unavailable"));

        assertThatThrownBy(() -> service().getStandings(1365L, 15L, false))
                .isInstanceOf(FlaClientException.class)
                .hasMessage("FLA service unavailable");
    }

    @Test
    void savesEmptyStandings() {
        when(externalDataSnapshotRepository.findTopByProviderAndChampionnatIdAndSaisonIdOrderByFetchedAtDescIdDesc("FLA", 1365L, 15L))
                .thenReturn(Optional.empty());
        when(flaClient.getStandings(1365L, 15L)).thenReturn(List.of());
        when(externalDataSnapshotWriter.saveOrUpdateFetchedAt(eq("FLA"), eq(1365L), eq(15L), any(), any(), any()))
                .thenReturn(new ExternalDataSnapshotWriter.SnapshotWriteResult(snapshot(11L), false));

        StandingsSnapshotResult result = service().getStandings(1365L, 15L, false);

        assertThat(result.cacheStatus()).isEqualTo(CacheStatus.MISS);
        assertThat(result.standings()).isEmpty();
    }

    @Test
    void concurrentUniqueConstraintConflictAfterReloadSucceeds() {
        when(externalDataSnapshotRepository.findTopByProviderAndChampionnatIdAndSaisonIdOrderByFetchedAtDescIdDesc("FLA", 1365L, 15L))
                .thenReturn(Optional.empty());
        when(flaClient.getStandings(1365L, 15L)).thenReturn(List.of(entry(6580L, 20)));
        when(externalDataSnapshotWriter.saveOrUpdateFetchedAt(eq("FLA"), eq(1365L), eq(15L), any(), any(), any()))
                .thenReturn(new ExternalDataSnapshotWriter.SnapshotWriteResult(snapshot(12L), true));

        StandingsSnapshotResult result = service().getStandings(1365L, 15L, false);

        assertThat(result.cacheStatus()).isEqualTo(CacheStatus.REUSED_IDENTICAL_PAYLOAD);
        assertThat(result.snapshotId()).isEqualTo(12L);
    }

    private FlaStandingsSnapshotServiceImpl service() {
        FlaProperties properties = new FlaProperties();
        properties.setSnapshotTtl(Duration.ofHours(6));
        var objectMapper = new JacksonConfig().objectMapper();
        return new FlaStandingsSnapshotServiceImpl(
                externalDataSnapshotRepository,
                externalDataSnapshotWriter,
                flaClient,
                new SnapshotPayloadHasher(objectMapper),
                objectMapper,
                properties
        );
    }

    private ExternalDataSnapshot snapshot(Long id) {
        return snapshot(id, OffsetDateTime.parse("2026-08-05T12:00:00Z"), rawPayload(entry(6580L, 20)), payloadHash());
    }

    private ExternalDataSnapshot snapshot(Long id, OffsetDateTime fetchedAt, String rawPayload, String payloadHash) {
        ExternalDataSnapshot snapshot = new ExternalDataSnapshot();
        snapshot.setId(id);
        snapshot.setProvider("FLA");
        snapshot.setChampionnatId(1365L);
        snapshot.setSaisonId(15L);
        snapshot.setPayloadHash(payloadHash);
        snapshot.setRawPayload(rawPayload);
        snapshot.setFetchedAt(fetchedAt);
        snapshot.setCreatedAt(fetchedAt.minusMinutes(1));
        return snapshot;
    }

    private String rawPayload(FlaStandingEntryResponse... entries) {
        try {
            return new JacksonConfig().objectMapper().writeValueAsString(List.of(entries));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String payloadHash() {
        return "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    }

    private FlaStandingEntryResponse entry(Long teamId, Integer points) {
        FlaStandingEntryResponse entry = new FlaStandingEntryResponse();
        entry.setTeamId(teamId);
        entry.setTeamName("Team " + teamId);
        entry.setChampionnatId(1365L);
        entry.setPoints(points);
        entry.setWins(1);
        entry.setDraws(0);
        entry.setLosses(0);
        entry.setGoalsFor(3);
        entry.setGoalsAgainst(1);
        entry.setBonus(0);
        entry.setForfeits(0);
        entry.setForm(List.of("won", "lost"));
        return entry;
    }
}
