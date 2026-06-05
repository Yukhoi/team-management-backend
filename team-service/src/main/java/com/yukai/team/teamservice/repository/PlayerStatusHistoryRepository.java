package com.yukai.team.teamservice.repository;

import com.yukai.team.teamservice.entity.PlayerStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerStatusHistoryRepository extends JpaRepository<PlayerStatusHistory, Long> {

    List<PlayerStatusHistory> findByPlayerIdOrderByChangedAtAsc(Long playerId);
}
