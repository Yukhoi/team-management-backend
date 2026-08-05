package com.yukai.team.matchservice.opponentanalysis.repository;

import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface FlaTeamRepository extends JpaRepository<FlaTeam, Long>, JpaSpecificationExecutor<FlaTeam> {

    List<FlaTeam> findByChampionnatIdAndSaisonIdOrderByTeamNameAsc(Long championnatId, Long saisonId);

    List<FlaTeam> findByChampionnatIdAndSaisonIdAndFlaTeamIdIn(Long championnatId, Long saisonId, List<Long> flaTeamIds);

    List<FlaTeam> findByChampionnatIdOrderByTeamNameAsc(Long championnatId);

    List<FlaTeam> findByFlaTeamIdOrderByTeamNameAsc(Long flaTeamId);

    Optional<FlaTeam> findByChampionnatIdAndSaisonIdAndFlaTeamId(Long championnatId, Long saisonId, Long flaTeamId);
}
