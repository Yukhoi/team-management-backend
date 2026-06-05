package com.yukai.team.teamservice.repository;

import com.yukai.team.teamservice.entity.OutboxEvent;
import com.yukai.team.teamservice.entity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    List<OutboxEvent> findTop100ByStatusInOrderByCreatedAtAsc(Collection<OutboxEventStatus> statuses);

    List<OutboxEvent> findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
            Collection<OutboxEventStatus> statuses,
            int retryCount
    );
}
