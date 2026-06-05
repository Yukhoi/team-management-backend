package com.yukai.team.matchservice.repository;

import com.yukai.team.matchservice.entity.MatchAppearance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchAppearanceRepository extends JpaRepository<MatchAppearance, Long> {

    List<MatchAppearance> findByMatchIdOrderByStarterDescPlayerIdAsc(Long matchId);

    Optional<MatchAppearance> findByMatchIdAndPlayerId(Long matchId, Long playerId);

    boolean existsByMatchIdAndPlayerId(Long matchId, Long playerId);

    void deleteByMatchId(Long matchId);
}
