package com.yukai.team.matchservice.service;

import com.yukai.team.matchservice.dto.response.TournamentSnapshotResponse;
import com.yukai.team.matchservice.repository.MatchInfoRepository;
import com.yukai.team.matchservice.repository.TournamentSyncStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentSyncServiceImpl implements TournamentSyncService {
    public static final String SYNC_EVENT_TYPE = "tournament.metadata.synchronized";
    private final TournamentSyncStateRepository states;
    private final MatchInfoRepository matches;
    private final OutboxEventService outbox;

    @Override
    @Transactional
    public void synchronizeTournament(Long id, String name, String season, Long version) {
        apply(id, name, season, version, false);
    }

    @Override
    @Transactional
    public void reconcileTournament(TournamentSnapshotResponse snapshot) {
        apply(snapshot.getId(), snapshot.getName(), snapshot.getSeason(), snapshot.getVersion(), true);
    }

    @Override
    @Transactional
    public TournamentSnapshotResponse prepareForCreation(TournamentSnapshotResponse snapshot) {
        apply(snapshot.getId(), snapshot.getName(), snapshot.getSeason(), snapshot.getVersion(), false);
        // Keep the same lock until createMatch commits, including its match.created Outbox row.
        var latest = states.lock(snapshot.getId());
        snapshot.setName(latest.name());
        snapshot.setSeason(latest.season());
        snapshot.setVersion(latest.version());
        return snapshot;
    }

    private void apply(Long id, String name, String season, Long version, boolean repair) {
        validate(id, name, season, version);
        var previous = states.lock(id);
        if (version < previous.version()) {
            log.info("Skipped stale tournament metadata, tournamentId={}, incomingVersion={}, appliedVersion={}", id, version, previous.version());
            return;
        }
        if (version == previous.version()) {
            if (!Objects.equals(name, previous.name()) || !Objects.equals(season, previous.season())) {
                throw new IllegalArgumentException("Conflicting tournament metadata for equal version: " + id);
            }
            if (!repair) return;
        }
        int updated = matches.updateTournamentSnapshots(id, name, season, OffsetDateTime.now());
        states.save(id, name, season, version);
        // Statistics receives only committed, versioned metadata through the existing Match Outbox.
        // Equal-version reconciliation deliberately republishes so existing statistics can also be repaired.
        outbox.saveEvent("tournament", id, SYNC_EVENT_TYPE, new Metadata(id, name, season, version));
        log.info("Tournament metadata synchronized, tournamentId={}, version={}, matchesUpdated={}, reconciliation={}", id, version, updated, repair);
    }

    public static void validate(Long id, String name, String season, Long version) {
        if (id == null || id <= 0 || version == null || version < 0
                || name == null || name.isBlank() || name.length() > 100
                || season == null || season.isBlank() || season.length() > 30) {
            throw new IllegalArgumentException("Tournament metadata requires positive id, name, season and nonnegative persisted version");
        }
    }

    public record Metadata(Long tournamentId, String name, String season, Long version) { }
}
