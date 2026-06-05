package com.yukai.team.teamservice.repository;

import com.yukai.team.teamservice.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByName(String name);

    Optional<Team> findByNameAndIsOurTeamFalse(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<Team> findByIsOurTeamTrue();

    boolean existsByIsOurTeamTrue();

    boolean existsByIsOurTeamTrueAndIdNot(Long id);

    List<Team> findAllByOrderByIdAsc();
}
