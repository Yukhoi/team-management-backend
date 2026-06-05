package com.yukai.team.statisticsservice.service.query;

import com.yukai.team.statisticsservice.dto.response.DashboardStatisticsResponse;
import com.yukai.team.statisticsservice.entity.LeaderboardProjection;
import com.yukai.team.statisticsservice.entity.TeamStatsProjection;
import com.yukai.team.statisticsservice.entity.enums.LeaderboardBoardType;
import com.yukai.team.statisticsservice.repository.LeaderboardProjectionRepository;
import com.yukai.team.statisticsservice.repository.TeamStatsProjectionRepository;
import com.yukai.team.statisticsservice.service.cache.StatisticsCacheKeyBuilder;
import com.yukai.team.statisticsservice.service.cache.StatisticsCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardStatisticsQueryService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final TeamStatsProjectionRepository teamStatsProjectionRepository;
    private final LeaderboardProjectionRepository leaderboardProjectionRepository;
    private final StatisticsCacheService statisticsCacheService;

    @Transactional(readOnly = true)
    public DashboardStatisticsResponse getDashboard() {
        String cacheKey = StatisticsCacheKeyBuilder.dashboard();
        Optional<DashboardStatisticsResponse> cached =
                statisticsCacheService.get(cacheKey, DashboardStatisticsResponse.class);
        if (cached.isPresent()) {
            log.info("Dashboard Cache HIT, key={}", cacheKey);
            return cached.get();
        }
        log.info("Dashboard Cache MISS, key={}", cacheKey);

        DashboardStatisticsResponse response = buildDashboard();
        statisticsCacheService.put(cacheKey, response, CACHE_TTL);
        return response;
    }

    private DashboardStatisticsResponse buildDashboard() {
        Optional<TeamStatsProjection> latestTeamStats = teamStatsProjectionRepository.findTopByOrderByUpdatedAtDesc();
        if (latestTeamStats.isEmpty()) {
            return DashboardStatisticsResponse.builder()
                    .totalMatches(0)
                    .wins(0)
                    .draws(0)
                    .losses(0)
                    .goals(0)
                    .goalsAgainst(0)
                    .topScorerGoals(0)
                    .topAssistCount(0)
                    .build();
        }

        TeamStatsProjection latest = latestTeamStats.get();
        TeamStatsProjection topTeam = teamStatsProjectionRepository
                .findBySeasonAndTournamentIdOrderByRankNoAsc(latest.getSeason(), latest.getTournamentId())
                .stream()
                .findFirst()
                .orElse(latest);

        LeaderboardProjection topScorer = topLeaderboard(
                LeaderboardBoardType.SCORER,
                latest.getSeason(),
                latest.getTournamentId()
        ).orElse(null);
        LeaderboardProjection topAssist = topLeaderboard(
                LeaderboardBoardType.ASSIST,
                latest.getSeason(),
                latest.getTournamentId()
        ).orElse(null);

        return DashboardStatisticsResponse.builder()
                .totalMatches(topTeam.getPlayed())
                .wins(topTeam.getWin())
                .draws(topTeam.getDraw())
                .losses(topTeam.getLose())
                .goals(topTeam.getGoalsFor())
                .goalsAgainst(topTeam.getGoalsAgainst())
                .topScorer(topScorer == null ? null : topScorer.getEntityNameSnapshot())
                .topScorerGoals(topScorer == null ? 0 : topScorer.getMetricValue())
                .topAssist(topAssist == null ? null : topAssist.getEntityNameSnapshot())
                .topAssistCount(topAssist == null ? 0 : topAssist.getMetricValue())
                .build();
    }

    private Optional<LeaderboardProjection> topLeaderboard(
            LeaderboardBoardType boardType,
            String season,
            Long tournamentId
    ) {
        List<LeaderboardProjection> projections = leaderboardProjectionRepository
                .findByBoardTypeAndSeasonAndTournamentIdOrderByRankNoAsc(boardType, season, tournamentId);
        return projections.stream().findFirst();
    }
}
