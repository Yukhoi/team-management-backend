package com.yukai.team.matchservice.repository;

import com.yukai.team.matchservice.entity.OutboxEvent;
import com.yukai.team.matchservice.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
            List<OutboxStatus> statuses,
            Integer maxRetryCount
    );
}
