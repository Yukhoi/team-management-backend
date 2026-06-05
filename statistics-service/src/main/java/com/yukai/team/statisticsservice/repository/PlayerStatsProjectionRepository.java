package com.yukai.team.statisticsservice.repository;

import com.yukai.team.statisticsservice.entity.PlayerStatsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerStatsProjectionRepository extends JpaRepository<PlayerStatsProjection, Long> {

    Optional<PlayerStatsProjection> findByPlayerIdAndSeasonAndTournamentId(
            Long playerId,
            String season,
            Long tournamentId
    );

    List<PlayerStatsProjection> findBySeasonAndTournamentId(String season, Long tournamentId);

    Page<PlayerStatsProjection> findBySeasonAndTournamentId(String season, Long tournamentId, Pageable pageable);
}
