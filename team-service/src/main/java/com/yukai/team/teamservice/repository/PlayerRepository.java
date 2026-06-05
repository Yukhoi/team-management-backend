package com.yukai.team.teamservice.repository;

import com.yukai.team.teamservice.entity.Player;
import com.yukai.team.teamservice.entity.PlayerCurrentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByIdAndDeletedFlagFalse(Long id);

    List<Player> findByDeletedFlagFalse();

    Page<Player> findByDeletedFlagFalse(Pageable pageable);

    List<Player> findByTeamIdAndDeletedFlagFalse(Long teamId);

    Page<Player> findByTeamIdAndDeletedFlagFalse(Long teamId, Pageable pageable);

    boolean existsByTeamIdAndDeletedFlagFalse(Long teamId);

    boolean existsByTeamIdAndJerseyNumberAndDeletedFlagFalseAndCurrentStatusNot(
            Long teamId,
            Integer jerseyNumber,
            PlayerCurrentStatus currentStatus
    );
}
