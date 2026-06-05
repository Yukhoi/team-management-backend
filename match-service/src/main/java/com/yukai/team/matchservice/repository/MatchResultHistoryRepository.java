package com.yukai.team.matchservice.repository;

import com.yukai.team.matchservice.entity.MatchResultHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchResultHistoryRepository extends JpaRepository<MatchResultHistory, Long> {
}
