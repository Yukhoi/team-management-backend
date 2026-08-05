package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.entity.ExternalDataSnapshot;

import java.time.OffsetDateTime;

public interface ExternalDataSnapshotWriter {

    SnapshotWriteResult saveOrUpdateFetchedAt(
            String provider,
            Long championnatId,
            Long saisonId,
            String payloadHash,
            String rawPayload,
            OffsetDateTime fetchedAt
    );

    record SnapshotWriteResult(ExternalDataSnapshot snapshot, boolean reusedIdenticalPayload) {
    }
}
