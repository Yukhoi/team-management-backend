package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.entity.ExternalDataSnapshot;
import com.yukai.team.matchservice.opponentanalysis.repository.ExternalDataSnapshotRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ExternalDataSnapshotWriterImpl implements ExternalDataSnapshotWriter {

    private final ExternalDataSnapshotRepository externalDataSnapshotRepository;

    public ExternalDataSnapshotWriterImpl(ExternalDataSnapshotRepository externalDataSnapshotRepository) {
        this.externalDataSnapshotRepository = externalDataSnapshotRepository;
    }

    @Override
    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public SnapshotWriteResult saveOrUpdateFetchedAt(
            String provider,
            Long championnatId,
            Long saisonId,
            String payloadHash,
            String rawPayload,
            OffsetDateTime fetchedAt
    ) {
        return externalDataSnapshotRepository
                .findByProviderAndChampionnatIdAndSaisonIdAndPayloadHash(
                        provider,
                        championnatId,
                        saisonId,
                        payloadHash
                )
                .map(snapshot -> new SnapshotWriteResult(updateFetchedAt(snapshot, fetchedAt), true))
                .orElseGet(() -> insertOrReload(provider, championnatId, saisonId, payloadHash, rawPayload, fetchedAt));
    }

    private SnapshotWriteResult insertOrReload(
            String provider,
            Long championnatId,
            Long saisonId,
            String payloadHash,
            String rawPayload,
            OffsetDateTime fetchedAt
    ) {
        try {
            ExternalDataSnapshot snapshot = new ExternalDataSnapshot();
            snapshot.setProvider(provider);
            snapshot.setChampionnatId(championnatId);
            snapshot.setSaisonId(saisonId);
            snapshot.setPayloadHash(payloadHash);
            snapshot.setRawPayload(rawPayload);
            snapshot.setFetchedAt(fetchedAt);
            return new SnapshotWriteResult(externalDataSnapshotRepository.saveAndFlush(snapshot), false);
        } catch (DataIntegrityViolationException exception) {
            ExternalDataSnapshot existing = externalDataSnapshotRepository
                    .findByProviderAndChampionnatIdAndSaisonIdAndPayloadHash(
                            provider,
                            championnatId,
                            saisonId,
                            payloadHash
                    )
                    .orElseThrow(() -> exception);
            return new SnapshotWriteResult(updateFetchedAt(existing, fetchedAt), true);
        }
    }

    private ExternalDataSnapshot updateFetchedAt(ExternalDataSnapshot snapshot, OffsetDateTime fetchedAt) {
        snapshot.setFetchedAt(fetchedAt);
        return externalDataSnapshotRepository.saveAndFlush(snapshot);
    }
}
