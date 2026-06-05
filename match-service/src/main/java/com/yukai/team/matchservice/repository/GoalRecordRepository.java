package com.yukai.team.matchservice.repository;

import com.yukai.team.matchservice.entity.GoalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalRecordRepository extends JpaRepository<GoalRecord, Long> {

    List<GoalRecord> findByMatchIdOrderByGoalMinuteAscIdAsc(Long matchId);
}
