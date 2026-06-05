package com.yukai.team.statisticsservice.repository;

import com.yukai.team.statisticsservice.entity.MatchPlayerAppearanceProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchPlayerAppearanceProjectionRepository
        extends JpaRepository<MatchPlayerAppearanceProjection, Long> {

    List<MatchPlayerAppearanceProjection> findByMatchId(Long matchId);

    void deleteByMatchId(Long matchId);
}
