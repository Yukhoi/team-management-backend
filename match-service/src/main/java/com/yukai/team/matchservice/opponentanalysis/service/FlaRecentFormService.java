package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.config.FlaProperties;
import com.yukai.team.matchservice.opponentanalysis.dto.RecentFormMetrics;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeamMapping;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaClientException;
import com.yukai.team.matchservice.opponentanalysis.exception.SnapshotProcessingException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class FlaRecentFormService {
    private final FlaTeamMatchesSnapshotService snapshots;
    private final RecentFormCalculator calculator;
    private final FlaProperties properties;

    public FlaRecentFormService(FlaTeamMatchesSnapshotService snapshots, RecentFormCalculator calculator, FlaProperties properties) {
        this.snapshots = snapshots;
        this.calculator = calculator;
        this.properties = properties;
    }

    public RecentFormMetrics getForm(FlaTeamMapping mapping, OffsetDateTime cutoff, boolean forceRefresh) {
        FlaTeamMatchesSnapshotService.Result snapshot;
        try {
            snapshot = snapshots.getTeamMatches(mapping.getFlaTeamId(), forceRefresh);
        } catch (FlaClientException exception) {
            return failure(mapping, forceRefresh, exception.getCode());
        } catch (SnapshotProcessingException | DataAccessException exception) {
            return failure(mapping, forceRefresh, "FLA_RECENT_SNAPSHOT_UNAVAILABLE");
        }
        var page = snapshot.page();
        if (page == null || !mapping.getFlaTeamId().equals(page.flaTeamId())) {
            return failure(mapping, forceRefresh, "FLA_INVALID_RESPONSE");
        }
        RecentFormMetrics.Source source = new RecentFormMetrics.Source(page.flaTeamId(), page.saisonId(),
                snapshot.payloadHash(), snapshot.payloadHash(), snapshot.fetchedAt(), snapshot.cacheStatus(),
                snapshot.cacheTtlSeconds(), snapshot.forceRefreshRequested(), null);
        // Season and championship identity remain provenance, not eligibility gates.
        return calculator.calculate(mapping.getFlaTeamId(), page.matches(), cutoff, source);
    }

    private RecentFormMetrics failure(FlaTeamMapping mapping, boolean forceRefresh, String code) {
        return RecentFormMetrics.unavailable(RecentFormMetrics.Status.SOURCE_UNAVAILABLE,
                new RecentFormMetrics.Source(mapping.getFlaTeamId(), null, null, null, null, null,
                        properties.getRecentMatchesTtl().toSeconds(), forceRefresh, code));
    }
}
