package com.yukai.team.matchservice.service;

import com.yukai.team.matchservice.client.TournamentClient;
import com.yukai.team.matchservice.repository.MatchInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentReconciliationService {
    private final MatchInfoRepository matches;
    private final TournamentClient client;
    private final TournamentSyncService sync;

    /** No outer transaction or HTTP under the database lock. Each tournament commits independently. */
    public void reconcileAll() {
        long after = 0;
        while (true) {
            var ids = matches.findTournamentIdsAfter(after, PageRequest.of(0, 100));
            if (ids.isEmpty()) break;
            for (Long id : ids) {
                var snapshot = client.getTournamentSnapshot(id);
                if (snapshot == null || !id.equals(snapshot.getId())) {
                    throw new IllegalStateException("Invalid reconciliation snapshot for tournament " + id);
                }
                sync.reconcileTournament(snapshot);
                after = id;
            }
        }
        log.info("Tournament reconciliation completed through tournamentId={}", after);
    }
}
