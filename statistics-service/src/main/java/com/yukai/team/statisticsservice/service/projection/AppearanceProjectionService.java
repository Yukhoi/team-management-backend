package com.yukai.team.statisticsservice.service.projection;

import com.yukai.team.statisticsservice.dto.event.data.MatchAppearanceUpdatedEventData;
import com.yukai.team.statisticsservice.entity.MatchPlayerAppearanceProjection;
import com.yukai.team.statisticsservice.entity.PlayerStatsProjection;
import com.yukai.team.statisticsservice.repository.MatchPlayerAppearanceProjectionRepository;
import com.yukai.team.statisticsservice.repository.PlayerStatsProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppearanceProjectionService {

    private final MatchPlayerAppearanceProjectionRepository matchPlayerAppearanceProjectionRepository;
    private final PlayerStatsProjectionService playerStatsProjectionService;
    private final PlayerStatsProjectionRepository playerStatsProjectionRepository;

    @Transactional
    public void rebuildMatchAppearance(MatchAppearanceUpdatedEventData data) {
        List<MatchPlayerAppearanceProjection> oldContributions =
                matchPlayerAppearanceProjectionRepository.findByMatchId(data.getMatchId());

        oldContributions.forEach(this::rollbackOldContribution);

        matchPlayerAppearanceProjectionRepository.deleteByMatchId(data.getMatchId());
        matchPlayerAppearanceProjectionRepository.flush();

        safeAppearances(data).stream()
                .filter(item -> Boolean.TRUE.equals(item.getAppeared()))
                .filter(item -> item.getPlayerId() != null)
                .forEach(item -> applyNewContribution(data, item));
    }

    private void rollbackOldContribution(MatchPlayerAppearanceProjection contribution) {
        playerStatsProjectionRepository.findByPlayerIdAndSeasonAndTournamentId(
                contribution.getPlayerId(),
                contribution.getSeason(),
                contribution.getTournamentId()
        ).ifPresent(stats -> {
            stats.setAppearances(nonNegativeDelta(stats.getAppearances(), -safe(contribution.getAppearanceCount())));
            stats.setStarts(nonNegativeDelta(stats.getStarts(), -safe(contribution.getStarterCount())));
            playerStatsProjectionRepository.save(stats);
        });
    }

    private void applyNewContribution(
            MatchAppearanceUpdatedEventData data,
            MatchAppearanceUpdatedEventData.AppearanceItem item
    ) {
        int starterCount = Boolean.TRUE.equals(item.getStarter()) ? 1 : 0;

        matchPlayerAppearanceProjectionRepository.save(MatchPlayerAppearanceProjection.builder()
                .matchId(data.getMatchId())
                .playerId(item.getPlayerId())
                .season(data.getSeasonSnapshot())
                .tournamentId(data.getTournamentId())
                .appearanceCount(1)
                .starterCount(starterCount)
                .build());

        PlayerStatsProjection stats = playerStatsProjectionService.getOrCreate(
                item.getPlayerId(),
                item.getPlayerNameSnapshot(),
                data.getSeasonSnapshot(),
                data.getTournamentId(),
                data.getTournamentNameSnapshot()
        );
        stats.setAppearances(safe(stats.getAppearances()) + 1);
        stats.setStarts(safe(stats.getStarts()) + starterCount);
        playerStatsProjectionRepository.save(stats);
    }

    private List<MatchAppearanceUpdatedEventData.AppearanceItem> safeAppearances(
            MatchAppearanceUpdatedEventData data
    ) {
        if (data.getAppearances() == null) {
            return Collections.emptyList();
        }
        return data.getAppearances();
    }

    private int nonNegativeDelta(Integer value, int delta) {
        return Math.max(0, safe(value) + delta);
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
