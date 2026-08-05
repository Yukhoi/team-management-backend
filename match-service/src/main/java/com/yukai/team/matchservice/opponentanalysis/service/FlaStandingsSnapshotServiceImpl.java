package com.yukai.team.matchservice.opponentanalysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.matchservice.opponentanalysis.client.FlaClient;
import com.yukai.team.matchservice.opponentanalysis.config.FlaProperties;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.entity.ExternalDataSnapshot;
import com.yukai.team.matchservice.opponentanalysis.repository.ExternalDataSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class FlaStandingsSnapshotServiceImpl implements FlaStandingsSnapshotService {

    public static final String PROVIDER_FLA = "FLA";

    private static final Logger log = LoggerFactory.getLogger(FlaStandingsSnapshotServiceImpl.class);
    private static final TypeReference<List<FlaStandingEntryResponse>> STANDINGS_TYPE = new TypeReference<>() {
    };

    private final ExternalDataSnapshotRepository externalDataSnapshotRepository;
    private final ExternalDataSnapshotWriter externalDataSnapshotWriter;
    private final FlaClient flaClient;
    private final SnapshotPayloadHasher snapshotPayloadHasher;
    private final ObjectMapper objectMapper;
    private final FlaProperties flaProperties;

    public FlaStandingsSnapshotServiceImpl(
            ExternalDataSnapshotRepository externalDataSnapshotRepository,
            ExternalDataSnapshotWriter externalDataSnapshotWriter,
            FlaClient flaClient,
            SnapshotPayloadHasher snapshotPayloadHasher,
            ObjectMapper objectMapper,
            FlaProperties flaProperties
    ) {
        this.externalDataSnapshotRepository = externalDataSnapshotRepository;
        this.externalDataSnapshotWriter = externalDataSnapshotWriter;
        this.flaClient = flaClient;
        this.snapshotPayloadHasher = snapshotPayloadHasher;
        this.objectMapper = objectMapper;
        this.flaProperties = flaProperties;
    }

    @Override
    public StandingsSnapshotResult getStandings(Long championnatId, Long saisonId, boolean forceRefresh) {
        OffsetDateTime now = OffsetDateTime.now();
        var latest = externalDataSnapshotRepository
                .findTopByProviderAndChampionnatIdAndSaisonIdOrderByFetchedAtDescIdDesc(
                        PROVIDER_FLA,
                        championnatId,
                        saisonId
                );
        boolean hadSnapshotBeforeFetch = latest.isPresent();
        if (!forceRefresh) {
            if (latest.isPresent() && isFresh(latest.get(), now)) {
                try {
                    return toResult(latest.get(), CacheStatus.HIT);
                } catch (IllegalStateException exception) {
                    log.warn(
                            "Ignoring corrupted FLA standings snapshot, snapshotId={}, championnatId={}, saisonId={}",
                            latest.get().getId(),
                            championnatId,
                            saisonId,
                            exception
                    );
                }
            }
        }

        List<FlaStandingEntryResponse> standings = flaClient.getStandings(championnatId, saisonId);
        OffsetDateTime fetchedAt = OffsetDateTime.now();
        SnapshotPayloadHasher.SnapshotPayload payload = snapshotPayloadHasher.hash(standings);
        ExternalDataSnapshotWriter.SnapshotWriteResult writeResult = externalDataSnapshotWriter.saveOrUpdateFetchedAt(
                PROVIDER_FLA,
                championnatId,
                saisonId,
                payload.payloadHash(),
                payload.rawPayload(),
                fetchedAt
        );
        CacheStatus cacheStatus = writeResult.reusedIdenticalPayload()
                ? CacheStatus.REUSED_IDENTICAL_PAYLOAD
                : statusForNewSnapshot(forceRefresh, hadSnapshotBeforeFetch);
        return new StandingsSnapshotResult(
                writeResult.snapshot().getId(),
                standings == null ? List.of() : standings,
                writeResult.snapshot().getPayloadHash(),
                writeResult.snapshot().getFetchedAt(),
                cacheStatus
        );
    }

    private CacheStatus statusForNewSnapshot(boolean forceRefresh, boolean hadSnapshotBeforeFetch) {
        if (forceRefresh || hadSnapshotBeforeFetch) {
            return CacheStatus.REFRESHED;
        }
        return CacheStatus.MISS;
    }

    private boolean isFresh(ExternalDataSnapshot snapshot, OffsetDateTime now) {
        return snapshot.getFetchedAt() != null
                && !snapshot.getFetchedAt().plus(flaProperties.getSnapshotTtl()).isBefore(now);
    }

    private StandingsSnapshotResult toResult(ExternalDataSnapshot snapshot, CacheStatus cacheStatus) {
        return new StandingsSnapshotResult(
                snapshot.getId(),
                parse(snapshot),
                snapshot.getPayloadHash(),
                snapshot.getFetchedAt(),
                cacheStatus
        );
    }

    private List<FlaStandingEntryResponse> parse(ExternalDataSnapshot snapshot) {
        try {
            List<FlaStandingEntryResponse> standings = objectMapper.readValue(snapshot.getRawPayload(), STANDINGS_TYPE);
            return standings == null ? List.of() : standings;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cached FLA standings snapshot cannot be parsed", exception);
        }
    }
}
