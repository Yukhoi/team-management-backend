package com.yukai.team.statisticsservice.repository;

import com.yukai.team.statisticsservice.entity.LeaderboardProjection;
import com.yukai.team.statisticsservice.entity.enums.LeaderboardBoardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaderboardProjectionRepository extends JpaRepository<LeaderboardProjection, Long> {

    List<LeaderboardProjection> findByBoardTypeAndSeasonAndTournamentIdOrderByRankNoAsc(
            LeaderboardBoardType boardType,
            String season,
            Long tournamentId
    );

    void deleteByBoardTypeAndSeasonAndTournamentId(
            LeaderboardBoardType boardType,
            String season,
            Long tournamentId
    );
}
