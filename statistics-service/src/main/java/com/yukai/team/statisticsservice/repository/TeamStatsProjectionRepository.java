package com.yukai.team.statisticsservice.repository;

import com.yukai.team.statisticsservice.entity.TeamStatsProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamStatsProjectionRepository extends JpaRepository<TeamStatsProjection, Long> {

    Optional<TeamStatsProjection> findByTeamIdAndSeasonAndTournamentId(
            Long teamId,
            String season,
            Long tournamentId
    );

    List<TeamStatsProjection> findBySeasonAndTournamentId(String season, Long tournamentId);

    List<TeamStatsProjection> findBySeasonAndTournamentIdOrderByRankNoAsc(String season, Long tournamentId);

    Optional<TeamStatsProjection> findTopByOrderByUpdatedAtDesc();
}
