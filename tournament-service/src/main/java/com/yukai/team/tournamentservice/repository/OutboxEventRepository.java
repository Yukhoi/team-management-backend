package com.yukai.team.tournamentservice.repository;

import com.yukai.team.tournamentservice.entity.OutboxEvent;
import com.yukai.team.tournamentservice.entity.enums.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    List<OutboxEvent> findTop100ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxEventStatus status,
            Integer retryCount
    );

    List<OutboxEvent> findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
            List<OutboxEventStatus> statuses,
            Integer retryCount
    );
}
