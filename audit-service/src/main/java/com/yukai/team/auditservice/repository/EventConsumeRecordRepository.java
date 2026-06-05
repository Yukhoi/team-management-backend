package com.yukai.team.auditservice.repository;

import com.yukai.team.auditservice.entity.EventConsumeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EventConsumeRecordRepository extends JpaRepository<EventConsumeRecord, Long> {

    boolean existsByEventIdAndConsumerName(UUID eventId, String consumerName);

    @Modifying
    @Query(
            value = """
                    insert into audit.event_consume_record (event_id, consumer_name, consumed_at)
                    values (:eventId, :consumerName, now())
                    on conflict (event_id, consumer_name) do nothing
                    """,
            nativeQuery = true
    )
    int insertIgnore(
            @Param("eventId") UUID eventId,
            @Param("consumerName") String consumerName
    );
}
