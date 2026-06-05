package com.yukai.team.statisticsservice.service.projection;

import com.yukai.team.statisticsservice.entity.LeaderboardProjection;
import com.yukai.team.statisticsservice.entity.PlayerStatsProjection;
import com.yukai.team.statisticsservice.entity.enums.LeaderboardBoardType;
import com.yukai.team.statisticsservice.repository.LeaderboardProjectionRepository;
import com.yukai.team.statisticsservice.repository.PlayerStatsProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;

@Service
@RequiredArgsConstructor
public class LeaderboardProjectionService {

    private final PlayerStatsProjectionRepository playerStatsProjectionRepository;
    private final LeaderboardProjectionRepository leaderboardProjectionRepository;

    @Transactional
    public void rebuildLeaderboards(String season, Long tournamentId) {
        List<PlayerStatsProjection> playerStats =
                playerStatsProjectionRepository.findBySeasonAndTournamentId(season, tournamentId);

        rebuildBoard(LeaderboardBoardType.SCORER, season, tournamentId, playerStats, PlayerStatsProjection::getGoals);
        rebuildBoard(LeaderboardBoardType.ASSIST, season, tournamentId, playerStats, PlayerStatsProjection::getAssists);
        rebuildBoard(
                LeaderboardBoardType.APPEARANCE,
                season,
                tournamentId,
                playerStats,
                PlayerStatsProjection::getAppearances
        );
        rebuildBoard(
                LeaderboardBoardType.GOAL_INVOLVEMENT,
                season,
                tournamentId,
                playerStats,
                PlayerStatsProjection::getGoalInvolvements
        );
    }

    private void rebuildBoard(
            LeaderboardBoardType boardType,
            String season,
            Long tournamentId,
            List<PlayerStatsProjection> playerStats,
            ToIntFunction<PlayerStatsProjection> metricExtractor
    ) {
        leaderboardProjectionRepository.deleteByBoardTypeAndSeasonAndTournamentId(boardType, season, tournamentId);
        leaderboardProjectionRepository.flush();

        AtomicInteger rank = new AtomicInteger(1);
        List<LeaderboardProjection> entries = playerStats.stream()
                .filter(stat -> metricExtractor.applyAsInt(stat) > 0)
                .sorted(Comparator
                        .comparingInt(metricExtractor)
                        .reversed()
                        .thenComparing(PlayerStatsProjection::getPlayerId))
                .map(stat -> LeaderboardProjection.builder()
                        .boardType(boardType)
                        .season(season)
                        .tournamentId(tournamentId)
                        .rankNo(rank.getAndIncrement())
                        .entityId(stat.getPlayerId())
                        .entityNameSnapshot(stat.getPlayerNameSnapshot())
                        .metricValue(metricExtractor.applyAsInt(stat))
                        .build())
                .toList();
        leaderboardProjectionRepository.saveAll(entries);
    }
}
