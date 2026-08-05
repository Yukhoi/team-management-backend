package com.yukai.team.matchservice.opponentanalysis.service;

public interface FlaStandingsSnapshotService {

    StandingsSnapshotResult getStandings(Long championnatId, Long saisonId, boolean forceRefresh);
}
