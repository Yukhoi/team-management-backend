package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;

import java.time.OffsetDateTime;
import java.util.List;

public record StandingsSnapshotResult(
        Long snapshotId,
        List<FlaStandingEntryResponse> standings,
        String payloadHash,
        OffsetDateTime fetchedAt,
        CacheStatus cacheStatus
) {
}
