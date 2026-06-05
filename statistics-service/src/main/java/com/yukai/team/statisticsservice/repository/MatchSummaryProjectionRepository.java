package com.yukai.team.statisticsservice.repository;

import com.yukai.team.statisticsservice.entity.MatchSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchSummaryProjectionRepository extends JpaRepository<MatchSummaryProjection, Long> {

    Optional<MatchSummaryProjection> findByMatchId(Long matchId);

    Page<MatchSummaryProjection> findAllByOrderByMatchTimeDesc(Pageable pageable);

    Page<MatchSummaryProjection> findByTournamentIdOrderByMatchTimeDesc(Long tournamentId, Pageable pageable);
}
