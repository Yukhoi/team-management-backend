package com.yukai.team.matchservice.repository;

import com.yukai.team.matchservice.entity.AssistRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssistRecordRepository extends JpaRepository<AssistRecord, Long> {

    Optional<AssistRecord> findByGoalRecordId(Long goalRecordId);

    boolean existsByGoalRecordId(Long goalRecordId);

    void deleteByGoalRecordId(Long goalRecordId);
}
