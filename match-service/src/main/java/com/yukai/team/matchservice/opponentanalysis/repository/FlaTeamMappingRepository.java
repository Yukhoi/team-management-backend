package com.yukai.team.matchservice.opponentanalysis.repository;

import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeamMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface FlaTeamMappingRepository extends JpaRepository<FlaTeamMapping, Long>, JpaSpecificationExecutor<FlaTeamMapping> {

    Optional<FlaTeamMapping> findByInternalTournamentIdAndInternalTeamId(Long internalTournamentId, Long internalTeamId);

    List<FlaTeamMapping> findByInternalTournamentIdOrderByInternalTeamIdAsc(Long internalTournamentId);

    boolean existsByInternalTournamentIdAndFlaChampionnatIdAndFlaSaisonIdAndFlaTeamIdAndInternalTeamIdNot(
            Long internalTournamentId,
            Long flaChampionnatId,
            Long flaSaisonId,
            Long flaTeamId,
            Long internalTeamId
    );

    Optional<FlaTeamMapping> findByFlaChampionnatIdAndFlaSaisonIdAndFlaTeamId(
            Long flaChampionnatId,
            Long flaSaisonId,
            Long flaTeamId
    );
}
