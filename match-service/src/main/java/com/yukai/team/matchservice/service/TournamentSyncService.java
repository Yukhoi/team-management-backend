package com.yukai.team.matchservice.service;

import com.yukai.team.matchservice.dto.response.TournamentSnapshotResponse;

public interface TournamentSyncService {
    void synchronizeTournament(Long tournamentId, String name, String season, Long version);
    void reconcileTournament(TournamentSnapshotResponse snapshot);
    TournamentSnapshotResponse prepareForCreation(TournamentSnapshotResponse snapshot);
}
