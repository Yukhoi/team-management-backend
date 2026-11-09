package com.yukai.team.matchservice.opponentanalysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.yukai.team.matchservice.opponentanalysis.client.FlaClient;
import com.yukai.team.matchservice.opponentanalysis.config.FlaProperties;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchesPageResponse;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeamMatchesSnapshot;
import com.yukai.team.matchservice.opponentanalysis.exception.SnapshotProcessingException;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaTeamMatchesSnapshotRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Service
public class FlaTeamMatchesSnapshotService {
    private final FlaTeamMatchesSnapshotRepository repository;
    private final FlaTeamMatchesSnapshotWriter writer;
    private final FlaClient client;
    private final FlaProperties properties;
    private final ObjectMapper mapper;

    public FlaTeamMatchesSnapshotService(FlaTeamMatchesSnapshotRepository repository, FlaTeamMatchesSnapshotWriter writer,
                                         FlaClient client, FlaProperties properties, ObjectMapper mapper) {
        this.repository = repository;
        this.writer = writer;
        this.client = client;
        this.properties = properties;
        this.mapper = mapper.copy().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public Result getTeamMatches(Long teamId, boolean forceRefresh) {
        if (teamId == null || teamId <= 0) {
            throw new IllegalArgumentException("flaTeamId must be positive");
        }
        OffsetDateTime now = OffsetDateTime.now();
        var latest = repository.findTopByFlaTeamIdOrderByFetchedAtDescPayloadHashDesc(teamId);
        if (!forceRefresh && latest.isPresent()
                && latest.get().getFetchedAt().plus(properties.getRecentMatchesTtl()).isAfter(now)) {
            // Corrupt caches fail explicitly; they must never become empty match lists.
            return result(latest.get(), CacheStatus.HIT, forceRefresh, teamId);
        }
        FlaTeamMatchesPageResponse page = client.getTeamMatchesPage(teamId);
        if (page == null || !teamId.equals(page.flaTeamId())) {
            throw new SnapshotProcessingException("Recent matches source team identity mismatch", null);
        }
        CanonicalPayload payload = canonical(page);
        OffsetDateTime fetchedAt = OffsetDateTime.now();
        var identical = repository.findById(payload.hash());
        if (identical.isPresent()) {
            return result(writer.touch(payload.hash(), fetchedAt), CacheStatus.REUSED_IDENTICAL_PAYLOAD, forceRefresh, teamId);
        }
        FlaTeamMatchesSnapshot snapshot = new FlaTeamMatchesSnapshot();
        snapshot.setFlaTeamId(teamId);
        snapshot.setSourceSaisonId(page.saisonId());
        snapshot.setPayloadHash(payload.hash());
        snapshot.setRawPayload(payload.json());
        snapshot.setFetchedAt(fetchedAt);
        try {
            writer.insert(snapshot);
        } catch (DataIntegrityViolationException exception) {
            // The writer's transaction has already rolled back; a concurrent winner may now be reloaded.
            if (repository.findById(payload.hash()).isEmpty()) {
                throw exception;
            }
            return result(writer.touch(payload.hash(), fetchedAt), CacheStatus.REUSED_IDENTICAL_PAYLOAD, forceRefresh, teamId);
        }
        return result(snapshot, latest.isPresent() || forceRefresh ? CacheStatus.REFRESHED : CacheStatus.MISS,
                forceRefresh, teamId);
    }

    CanonicalPayload canonical(FlaTeamMatchesPageResponse page) {
        try {
            List<FlaTeamMatchResponse> matches = page.matches().stream().distinct()
                    .sorted(Comparator.comparing(this::json)).toList();
            String json = mapper.writeValueAsString(new Payload(1,
                    new FlaTeamMatchesPageResponse(page.flaTeamId(), page.championnatId(), page.saisonId(), matches)));
            return new CanonicalPayload(json, hash(json));
        } catch (JsonProcessingException exception) {
            throw new SnapshotProcessingException("Failed to serialize recent matches snapshot", exception);
        }
    }

    private Result result(FlaTeamMatchesSnapshot snapshot, CacheStatus status, boolean forceRefresh, Long teamId) {
        try {
            Payload payload = mapper.readValue(snapshot.getRawPayload(), Payload.class);
            if (payload == null || payload.schemaVersion() != 1 || payload.page() == null
                    || !teamId.equals(snapshot.getFlaTeamId())
                    || !teamId.equals(payload.page().flaTeamId())
                    || !java.util.Objects.equals(snapshot.getSourceSaisonId(), payload.page().saisonId())
                    || !hash(snapshot.getRawPayload()).equals(snapshot.getPayloadHash())) {
                throw new SnapshotProcessingException("Invalid recent matches cache identity or payload", null);
            }
            return new Result(payload.page(), snapshot.getPayloadHash(), snapshot.getFetchedAt(), status,
                    properties.getRecentMatchesTtl().toSeconds(), forceRefresh);
        } catch (JsonProcessingException exception) {
            throw new SnapshotProcessingException("Failed to read recent matches snapshot", exception);
        }
    }

    private String json(FlaTeamMatchResponse match) {
        try {
            return mapper.writeValueAsString(match);
        } catch (JsonProcessingException exception) {
            throw new SnapshotProcessingException("Failed to serialize recent match", exception);
        }
    }

    private String hash(String json) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new SnapshotProcessingException("SHA-256 is unavailable", exception);
        }
    }

    public record Payload(int schemaVersion, FlaTeamMatchesPageResponse page) { }
    record CanonicalPayload(String json, String hash) { }
    public record Result(FlaTeamMatchesPageResponse page, String payloadHash, OffsetDateTime fetchedAt,
                         CacheStatus cacheStatus, long cacheTtlSeconds, boolean forceRefreshRequested) { }
}
