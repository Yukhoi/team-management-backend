package com.yukai.team.matchservice.service;

import com.yukai.team.matchservice.client.TournamentClient;
import com.yukai.team.matchservice.repository.MatchInfoRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class TournamentReconciliationServiceTest {
    final MatchInfoRepository matches = mock(MatchInfoRepository.class);
    final TournamentClient client = mock(TournamentClient.class);
    final TournamentSyncService sync = mock(TournamentSyncService.class);
    final TournamentReconciliationService service = new TournamentReconciliationService(matches,client,sync);
    @Test
    void readsDistinctTournamentPagesAndUsesVersionedAuthoritativeSnapshots() {
        var snapshot = TournamentSyncIntegrationTest.snapshot("Latest","2027",3L);
        when(matches.findTournamentIdsAfter(eq(0L),any())).thenReturn(List.of(5L));
        when(matches.findTournamentIdsAfter(eq(5L),any())).thenReturn(List.of());
        when(client.getTournamentSnapshot(5L)).thenReturn(snapshot);
        service.reconcileAll();
        verify(sync).reconcileTournament(snapshot);
        verify(matches).findTournamentIdsAfter(eq(5L),any());
    }
    @Test
    void authoritativeLookupFailureStopsRepairWithoutInventingMetadata() {
        when(matches.findTournamentIdsAfter(eq(0L),any())).thenReturn(List.of(5L));
        when(client.getTournamentSnapshot(5L)).thenThrow(new IllegalStateException("HTTP unavailable"));
        assertThatThrownBy(service::reconcileAll).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(sync);
    }
}
