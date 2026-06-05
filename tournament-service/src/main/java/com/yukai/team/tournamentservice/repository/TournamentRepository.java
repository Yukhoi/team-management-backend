package com.yukai.team.tournamentservice.repository;

import com.yukai.team.tournamentservice.entity.Tournament;
import com.yukai.team.tournamentservice.entity.enums.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long>, JpaSpecificationExecutor<Tournament> {

    Optional<Tournament> findByIdAndStatus(Long id, TournamentStatus status);

    boolean existsByNameAndSeason(String name, String season);
}
