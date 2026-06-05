package com.yukai.team.statisticsservice.service.projection;

import com.yukai.team.statisticsservice.dto.event.data.MatchAssistDeletedEventData;
import com.yukai.team.statisticsservice.dto.event.data.MatchAssistUpsertedEventData;
import com.yukai.team.statisticsservice.dto.event.data.MatchGoalCreatedEventData;
import com.yukai.team.statisticsservice.dto.event.data.MatchGoalDeletedEventData;
import com.yukai.team.statisticsservice.dto.event.data.MatchGoalUpdatedEventData;
import com.yukai.team.statisticsservice.entity.PlayerStatsProjection;
import com.yukai.team.statisticsservice.repository.PlayerStatsProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerStatsProjectionService {

    private static final String OWN_GOAL = "OWN_GOAL";

    private final PlayerStatsProjectionRepository playerStatsProjectionRepository;

    @Transactional
    public PlayerStatsProjection getOrCreate(
            Long playerId,
            String playerNameSnapshot,
            String season,
            Long tournamentId,
            String tournamentNameSnapshot
    ) {
        return playerStatsProjectionRepository.findByPlayerIdAndSeasonAndTournamentId(playerId, season, tournamentId)
                .map(existing -> updateSnapshots(existing, playerNameSnapshot, tournamentNameSnapshot))
                .orElseGet(() -> playerStatsProjectionRepository.save(PlayerStatsProjection.builder()
                        .playerId(playerId)
                        .playerNameSnapshot(playerNameSnapshot)
                        .season(season)
                        .tournamentId(tournamentId)
                        .tournamentNameSnapshot(tournamentNameSnapshot)
                        .appearances(0)
                        .starts(0)
                        .goals(0)
                        .assists(0)
                        .goalInvolvements(0)
                        .build()));
    }

    @Transactional
    public void applyGoalCreated(MatchGoalCreatedEventData eventData) {
        applyGoalDelta(
                eventData.getPlayerId(),
                eventData.getPlayerNameSnapshot(),
                eventData.getGoalType(),
                eventData.getSeasonSnapshot(),
                eventData.getTournamentId(),
                eventData.getTournamentNameSnapshot(),
                1
        );
    }

    @Transactional
    public void applyGoalDeleted(MatchGoalDeletedEventData eventData) {
        applyGoalDelta(
                eventData.getPlayerId(),
                eventData.getPlayerNameSnapshot(),
                eventData.getGoalType(),
                eventData.getSeasonSnapshot(),
                eventData.getTournamentId(),
                eventData.getTournamentNameSnapshot(),
                -1
        );
        if (eventData.getLinkedAssist() != null) {
            applyAssistDelta(
                    eventData.getLinkedAssist().getPlayerId(),
                    eventData.getLinkedAssist().getPlayerNameSnapshot(),
                    eventData.getSeasonSnapshot(),
                    eventData.getTournamentId(),
                    eventData.getTournamentNameSnapshot(),
                    -1
            );
        }
    }

    @Transactional
    public void applyGoalUpdated(MatchGoalUpdatedEventData eventData) {
        if (eventData.getOldGoal() != null) {
            applyGoalDelta(
                    eventData.getOldGoal().getPlayerId(),
                    eventData.getOldGoal().getPlayerNameSnapshot(),
                    eventData.getOldGoal().getGoalType(),
                    eventData.getSeasonSnapshot(),
                    eventData.getTournamentId(),
                    eventData.getTournamentNameSnapshot(),
                    -1
            );
        }
        if (eventData.getNewGoal() != null) {
            applyGoalDelta(
                    eventData.getNewGoal().getPlayerId(),
                    eventData.getNewGoal().getPlayerNameSnapshot(),
                    eventData.getNewGoal().getGoalType(),
                    eventData.getSeasonSnapshot(),
                    eventData.getTournamentId(),
                    eventData.getTournamentNameSnapshot(),
                    1
            );
        }
    }

    @Transactional
    public void applyAssistUpserted(MatchAssistUpsertedEventData eventData) {
        if (eventData.getOldAssist() != null) {
            applyAssistDelta(
                    eventData.getOldAssist().getPlayerId(),
                    eventData.getOldAssist().getPlayerNameSnapshot(),
                    eventData.getSeasonSnapshot(),
                    eventData.getTournamentId(),
                    eventData.getTournamentNameSnapshot(),
                    -1
            );
        }
        if (eventData.getNewAssist() != null) {
            applyAssistDelta(
                    eventData.getNewAssist().getPlayerId(),
                    eventData.getNewAssist().getPlayerNameSnapshot(),
                    eventData.getSeasonSnapshot(),
                    eventData.getTournamentId(),
                    eventData.getTournamentNameSnapshot(),
                    1
            );
        }
    }

    @Transactional
    public void applyAssistDeleted(MatchAssistDeletedEventData eventData) {
        applyAssistDelta(
                eventData.getPlayerId(),
                eventData.getPlayerNameSnapshot(),
                eventData.getSeasonSnapshot(),
                eventData.getTournamentId(),
                eventData.getTournamentNameSnapshot(),
                -1
        );
    }

    private void applyGoalDelta(
            Long playerId,
            String playerNameSnapshot,
            String goalType,
            String season,
            Long tournamentId,
            String tournamentNameSnapshot,
            int delta
    ) {
        if (playerId == null || OWN_GOAL.equals(goalType)) {
            return;
        }

        PlayerStatsProjection projection = getOrCreate(
                playerId,
                playerNameSnapshot,
                season,
                tournamentId,
                tournamentNameSnapshot
        );
        projection.setGoals(applyNonNegativeDelta(projection.getGoals(), delta));
        projection.recalculateGoalInvolvements();
        playerStatsProjectionRepository.save(projection);
    }

    private void applyAssistDelta(
            Long playerId,
            String playerNameSnapshot,
            String season,
            Long tournamentId,
            String tournamentNameSnapshot,
            int delta
    ) {
        if (playerId == null) {
            return;
        }

        PlayerStatsProjection projection = getOrCreate(
                playerId,
                playerNameSnapshot,
                season,
                tournamentId,
                tournamentNameSnapshot
        );
        projection.setAssists(applyNonNegativeDelta(projection.getAssists(), delta));
        projection.recalculateGoalInvolvements();
        playerStatsProjectionRepository.save(projection);
    }

    private PlayerStatsProjection updateSnapshots(
            PlayerStatsProjection projection,
            String playerNameSnapshot,
            String tournamentNameSnapshot
    ) {
        projection.setPlayerNameSnapshot(playerNameSnapshot);
        projection.setTournamentNameSnapshot(tournamentNameSnapshot);
        return projection;
    }

    private int applyNonNegativeDelta(Integer value, int delta) {
        return Math.max(0, (value == null ? 0 : value) + delta);
    }
}
