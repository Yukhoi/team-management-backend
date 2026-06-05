package com.yukai.team.statisticsservice.service.projection;

import com.yukai.team.statisticsservice.entity.TeamStatsProjection;
import com.yukai.team.statisticsservice.repository.TeamStatsProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class TeamRankingService {

    private final TeamStatsProjectionRepository teamStatsProjectionRepository;

    @Transactional
    public void rebuildTournamentRanking(String season, Long tournamentId) {
        List<TeamStatsProjection> teams = teamStatsProjectionRepository.findBySeasonAndTournamentId(season, tournamentId);

        AtomicInteger rank = new AtomicInteger(1);
        teams.stream()
                .sorted(Comparator
                        .comparing(TeamStatsProjection::getPoints, Comparator.reverseOrder())
                        .thenComparing(TeamStatsProjection::getGoalDiff, Comparator.reverseOrder())
                        .thenComparing(TeamStatsProjection::getGoalsFor, Comparator.reverseOrder())
                        .thenComparing(TeamStatsProjection::getTeamId))
                .forEach(team -> team.setRankNo(rank.getAndIncrement()));

        teamStatsProjectionRepository.saveAll(teams);
    }
}
